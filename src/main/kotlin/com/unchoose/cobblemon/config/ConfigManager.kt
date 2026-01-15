package com.unchoose.cobblemon.config

import com.unchoose.cobblemon.UnChooseCobblemon
import com.unchoose.cobblemon.util.ColorUtil
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.InputStreamReader

class ConfigManager(private val plugin: UnChooseCobblemon) {
    
    private lateinit var config: FileConfiguration
    private lateinit var langConfig: FileConfiguration
    private var currentLang = "zh_CN"
    
    fun loadConfig() {
        // 保存默认配置
        plugin.saveDefaultConfig()
        plugin.reloadConfig()
        config = plugin.config
        
        // 保存语言文件
        saveLangFiles()
        
        // 加载语言文件
        currentLang = config.getString("language", "zh_CN") ?: "zh_CN"
        loadLang(currentLang)
    }
    
    private fun saveLangFiles() {
        val langFolder = File(plugin.dataFolder, "lang")
        if (!langFolder.exists()) {
            langFolder.mkdirs()
        }
        
        listOf("zh_CN.yml", "en_US.yml").forEach { langFile ->
            val file = File(langFolder, langFile)
            if (!file.exists()) {
                plugin.getResource("lang/$langFile")?.let { stream ->
                    file.writeBytes(stream.readBytes())
                }
            }
        }
    }
    
    private fun loadLang(lang: String) {
        val langFile = File(plugin.dataFolder, "lang/$lang.yml")
        langConfig = if (langFile.exists()) {
            YamlConfiguration.loadConfiguration(langFile)
        } else {
            // 回退到默认语言
            val defaultFile = File(plugin.dataFolder, "lang/zh_CN.yml")
            if (defaultFile.exists()) {
                YamlConfiguration.loadConfiguration(defaultFile)
            } else {
                YamlConfiguration()
            }
        }
    }
    
    // ============ 数据库配置 ============
    
    fun getDatabaseType(): String = config.getString("database.type", "sqlite") ?: "sqlite"
    
    fun getSQLiteFile(): String = config.getString("database.sqlite.file", "data.db") ?: "data.db"
    
    fun getMySQLHost(): String = config.getString("database.mysql.host", "localhost") ?: "localhost"
    fun getMySQLPort(): Int = config.getInt("database.mysql.port", 3306)
    fun getMySQLDatabase(): String = config.getString("database.mysql.database", "unchoose_cobblemon") ?: "unchoose_cobblemon"
    fun getMySQLUsername(): String = config.getString("database.mysql.username", "root") ?: "root"
    fun getMySQLPassword(): String = config.getString("database.mysql.password", "") ?: ""
    
    fun getPoolMaxSize(): Int = config.getInt("database.mysql.pool.maximum-pool-size", 10)
    fun getPoolMinIdle(): Int = config.getInt("database.mysql.pool.minimum-idle", 2)
    fun getPoolMaxLifetime(): Long = config.getLong("database.mysql.pool.max-lifetime", 1800000)
    fun getPoolConnectionTimeout(): Long = config.getLong("database.mysql.pool.connection-timeout", 30000)
    
    // ============ 初始宝可梦配置 ============
    
    fun isStarterBlockEnabled(): Boolean = config.getBoolean("starter.enabled", true)
    fun getStarterBlockMode(): String = config.getString("starter.mode", "notify") ?: "notify"
    fun isAllowBypass(): Boolean = config.getBoolean("starter.allow-bypass", true)
    fun isAutoLockNewPlayers(): Boolean = config.getBoolean("starter.auto-lock-new-players", true)
    fun getMessageType(): String = config.getString("starter.message-type", "actionbar") ?: "actionbar"
    
    // ============ GUI配置 ============
    
    fun isGUIEnabled(): Boolean = config.getBoolean("gui.enabled", true)
    fun getMainMenuTitle(): String = ColorUtil.colorize(config.getString("gui.main-menu-title", "&6&l宝可梦初始选择管理") ?: "&6&l宝可梦初始选择管理")
    fun getPlayerMenuTitle(): String = ColorUtil.colorize(config.getString("gui.player-menu-title", "&b&l玩家管理") ?: "&b&l玩家管理")
    fun getPlayersPerPage(): Int = config.getInt("gui.players-per-page", 45)
    
    fun isFillerEnabled(): Boolean = config.getBoolean("gui.filler.enabled", true)
    fun getFillerMaterial(): String = config.getString("gui.filler.material", "GRAY_STAINED_GLASS_PANE") ?: "GRAY_STAINED_GLASS_PANE"
    fun getFillerName(): String = ColorUtil.colorize(config.getString("gui.filler.name", " ") ?: " ")
    
    fun getButtonMaterial(button: String): String = config.getString("gui.buttons.$button.material", "STONE") ?: "STONE"
    fun getButtonName(button: String): String = ColorUtil.colorize(config.getString("gui.buttons.$button.name", button) ?: button)
    
    // ============ 日志配置 ============
    
    fun isVerbose(): Boolean = config.getBoolean("logging.verbose", false)
    fun isLogAttempts(): Boolean = config.getBoolean("logging.log-attempts", true)
    fun getLogFile(): String = config.getString("logging.file", "logs/unchoose.log") ?: "logs/unchoose.log"
    
    // ============ 性能配置 ============
    
    fun isAsyncDatabase(): Boolean = config.getBoolean("performance.async-database", true)
    fun getCacheDuration(): Int = config.getInt("performance.cache-duration", 300)
    fun getMaxCacheSize(): Int = config.getInt("performance.max-cache-size", 1000)
    
    // ============ 消息获取 ============
    
    fun getMessage(path: String, vararg replacements: Pair<String, String>): String {
        var message = langConfig.getString(path) ?: config.getString("messages.$path", path) ?: path
        replacements.forEach { (key, value) ->
            message = message.replace("{$key}", value)
        }
        return ColorUtil.colorize(getPrefix() + message)
    }
    
    fun getRawMessage(path: String, vararg replacements: Pair<String, String>): String {
        var message = langConfig.getString(path) ?: config.getString("messages.$path", path) ?: path
        replacements.forEach { (key, value) ->
            message = message.replace("{$key}", value)
        }
        return ColorUtil.colorize(message)
    }
    
    fun getMessageList(path: String): List<String> {
        val list = langConfig.getStringList(path).ifEmpty { config.getStringList("messages.$path") }
        return list.map { ColorUtil.colorize(it) }
    }
    
    fun getPrefix(): String = ColorUtil.colorize(
        langConfig.getString("prefix") ?: config.getString("messages.prefix", "&8[&6UnChoose&8] &r") ?: ""
    )
}
