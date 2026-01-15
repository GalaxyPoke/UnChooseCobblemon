package com.unchoose.cobblemon

import com.unchoose.cobblemon.command.MainCommand
import com.unchoose.cobblemon.config.ConfigManager
import com.unchoose.cobblemon.database.DatabaseManager
import com.unchoose.cobblemon.listener.CobblemonEventListener
import com.unchoose.cobblemon.listener.PlayerListener
import com.unchoose.cobblemon.manager.PlayerDataManager
import com.unchoose.cobblemon.util.FileLogger
import com.unchoose.cobblemon.util.Logger
import net.byteflux.libby.BukkitLibraryManager
import net.byteflux.libby.Library
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class UnChooseCobblemon : JavaPlugin() {
    
    companion object {
        lateinit var instance: UnChooseCobblemon
            private set
    }
    
    lateinit var configManager: ConfigManager
        private set
    lateinit var databaseManager: DatabaseManager
        private set
    lateinit var playerDataManager: PlayerDataManager
        private set
    
    var cobblemonListener: CobblemonEventListener? = null
        private set
    
    lateinit var fileLogger: FileLogger
        private set
    
    private var cobblemonHooked = false
    
    override fun onEnable() {
        instance = this
        
        // 创建数据目录
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }
        
        // 使用libby加载运行时依赖
        loadDependencies()
        
        // 初始化配置
        Logger.info("正在加载配置...")
        configManager = ConfigManager(this)
        configManager.loadConfig()
        
        // 初始化数据库
        Logger.info("正在连接数据库...")
        databaseManager = DatabaseManager(this)
        if (!databaseManager.connect()) {
            Logger.severe("数据库连接失败，插件将被禁用！")
            server.pluginManager.disablePlugin(this)
            return
        }
        
        // 初始化玩家数据管理器
        playerDataManager = PlayerDataManager(this)
        
        // 初始化文件日志记录器
        fileLogger = FileLogger(this)
        
        // 检查Cobblemon依赖
        checkCobblemonHook()
        
        // 注册事件监听器
        registerListeners()
        
        // 注册命令
        registerCommands()
        
        Logger.info("UnChooseCobblemon v${description.version} 已启用！")
        if (cobblemonHooked) {
            Logger.info("已成功挂钩Cobblemon！")
        } else {
            Logger.warn("未检测到Cobblemon，部分功能可能不可用")
        }
    }
    
    private fun loadDependencies() {
        Logger.info("正在加载运行时依赖...")
        val libraryManager = BukkitLibraryManager(this)
        libraryManager.addMavenCentral()
        
        // Kotlin stdlib
        val kotlinStdlib = Library.builder()
            .groupId("org.jetbrains.kotlin")
            .artifactId("kotlin-stdlib")
            .version("1.9.22")
            .build()
        libraryManager.loadLibrary(kotlinStdlib)
        
        // HikariCP
        val hikari = Library.builder()
            .groupId("com.zaxxer")
            .artifactId("HikariCP")
            .version("5.1.0")
            .build()
        libraryManager.loadLibrary(hikari)
        
        // SQLite JDBC
        val sqlite = Library.builder()
            .groupId("org.xerial")
            .artifactId("sqlite-jdbc")
            .version("3.45.1.0")
            .build()
        libraryManager.loadLibrary(sqlite)
        
        // MySQL Connector
        val mysql = Library.builder()
            .groupId("com.mysql")
            .artifactId("mysql-connector-j")
            .version("8.3.0")
            .build()
        libraryManager.loadLibrary(mysql)
        
        Logger.info("运行时依赖加载完成！")
    }
    
    override fun onDisable() {
        // 保存所有缓存数据
        if (::playerDataManager.isInitialized) {
            playerDataManager.saveAllCache()
        }
        
        // 写入剩余日志
        if (::fileLogger.isInitialized) {
            fileLogger.flush()
        }
        
        // 关闭数据库连接
        if (::databaseManager.isInitialized) {
            databaseManager.disconnect()
        }
        
        Logger.info("UnChooseCobblemon 已禁用！")
    }
    
    private fun checkCobblemonHook() {
        // Cobblemon是模组，不是Bukkit插件，需要通过类检测
        cobblemonHooked = try {
            Class.forName("com.cobblemon.mod.common.Cobblemon")
            Logger.info("检测到Cobblemon模组")
            true
        } catch (e: ClassNotFoundException) {
            // 也尝试检查插件管理器（以防某些混合服务端将其注册为插件）
            server.pluginManager.getPlugin("Cobblemon") != null
        }
    }
    
    fun isCobblemonHooked(): Boolean = cobblemonHooked
    
    private fun registerListeners() {
        val pm = server.pluginManager
        pm.registerEvents(PlayerListener(this), this)
        
        if (cobblemonHooked) {
            cobblemonListener = CobblemonEventListener(this)
            pm.registerEvents(cobblemonListener!!, this)
        }
    }
    
    private fun registerCommands() {
        getCommand("unchoose")?.let { cmd ->
            val mainCommand = MainCommand(this)
            cmd.setExecutor(mainCommand)
            cmd.tabCompleter = mainCommand
        }
    }
    
    fun reload(): Boolean {
        return try {
            configManager.loadConfig()
            Logger.info("配置已重新加载")
            true
        } catch (e: Exception) {
            Logger.severe("配置重载失败: ${e.message}")
            false
        }
    }
}
