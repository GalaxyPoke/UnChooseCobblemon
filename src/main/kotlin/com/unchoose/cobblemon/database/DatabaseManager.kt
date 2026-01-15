package com.unchoose.cobblemon.database

import com.unchoose.cobblemon.UnChooseCobblemon
import com.unchoose.cobblemon.util.Logger
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.io.File
import java.sql.Connection
import java.sql.SQLException

class DatabaseManager(private val plugin: UnChooseCobblemon) {
    
    private var dataSource: HikariDataSource? = null
    private val config = plugin.configManager
    
    fun connect(): Boolean {
        return try {
            val hikariConfig = HikariConfig()
            
            when (config.getDatabaseType().lowercase()) {
                "mysql" -> configureMysql(hikariConfig)
                else -> configureSqlite(hikariConfig)
            }
            
            // 通用连接池配置
            hikariConfig.maximumPoolSize = config.getPoolMaxSize()
            hikariConfig.minimumIdle = config.getPoolMinIdle()
            hikariConfig.maxLifetime = config.getPoolMaxLifetime()
            hikariConfig.connectionTimeout = config.getPoolConnectionTimeout()
            hikariConfig.poolName = "UnChooseCobblemon-Pool"
            
            dataSource = HikariDataSource(hikariConfig)
            
            // 创建表结构
            createTables()
            
            Logger.info("数据库连接成功 (${config.getDatabaseType()})")
            true
        } catch (e: Exception) {
            Logger.error("数据库连接失败", e)
            false
        }
    }
    
    private fun configureSqlite(hikariConfig: HikariConfig) {
        val dbFile = File(plugin.dataFolder, config.getSQLiteFile())
        hikariConfig.jdbcUrl = "jdbc:sqlite:${dbFile.absolutePath}"
        hikariConfig.driverClassName = "org.sqlite.JDBC"
        
        // SQLite特定配置
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true")
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250")
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        
        // SQLite不支持多连接写入，限制连接池大小
        hikariConfig.maximumPoolSize = 1
    }
    
    private fun configureMysql(hikariConfig: HikariConfig) {
        val host = config.getMySQLHost()
        val port = config.getMySQLPort()
        val database = config.getMySQLDatabase()
        
        hikariConfig.jdbcUrl = "jdbc:mysql://$host:$port/$database?useSSL=false&autoReconnect=true&useUnicode=true&characterEncoding=UTF-8"
        hikariConfig.username = config.getMySQLUsername()
        hikariConfig.password = config.getMySQLPassword()
        hikariConfig.driverClassName = "com.mysql.cj.jdbc.Driver"
        
        // MySQL特定配置
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true")
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250")
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true")
        hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true")
    }
    
    private fun createTables() {
        getConnection()?.use { conn ->
            conn.createStatement().use { stmt ->
                // 玩家数据表
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS player_data (
                        uuid VARCHAR(36) PRIMARY KEY,
                        player_name VARCHAR(16) NOT NULL,
                        starter_locked BOOLEAN DEFAULT TRUE,
                        starter_selected BOOLEAN DEFAULT FALSE,
                        lock_reason TEXT,
                        locked_by VARCHAR(36),
                        locked_at BIGINT,
                        first_join BIGINT NOT NULL,
                        last_join BIGINT NOT NULL,
                        updated_at BIGINT NOT NULL
                    )
                """.trimIndent())
                
                // 日志表
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS action_log (
                        id INTEGER PRIMARY KEY ${if (config.getDatabaseType() == "mysql") "AUTO_INCREMENT" else "AUTOINCREMENT"},
                        player_uuid VARCHAR(36) NOT NULL,
                        action_type VARCHAR(32) NOT NULL,
                        action_by VARCHAR(36),
                        action_data TEXT,
                        timestamp BIGINT NOT NULL
                    )
                """.trimIndent())
                
                // 创建索引
                try {
                    stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_player_name ON player_data(player_name)")
                    stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_log_player ON action_log(player_uuid)")
                    stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_log_time ON action_log(timestamp)")
                } catch (e: SQLException) {
                    // 索引可能已存在，忽略
                    Logger.debug("索引创建跳过: ${e.message}")
                }
            }
        }
    }
    
    fun getConnection(): Connection? {
        return try {
            dataSource?.connection
        } catch (e: SQLException) {
            Logger.error("获取数据库连接失败", e)
            null
        }
    }
    
    fun disconnect() {
        dataSource?.close()
        dataSource = null
        Logger.info("数据库连接已关闭")
    }
    
    fun isConnected(): Boolean = dataSource != null && !dataSource!!.isClosed
}
