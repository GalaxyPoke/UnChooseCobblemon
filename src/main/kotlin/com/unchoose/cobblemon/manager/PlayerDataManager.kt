package com.unchoose.cobblemon.manager

import com.unchoose.cobblemon.UnChooseCobblemon
import com.unchoose.cobblemon.database.PlayerDAO
import com.unchoose.cobblemon.model.PlayerData
import com.unchoose.cobblemon.util.Logger
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class PlayerDataManager(private val plugin: UnChooseCobblemon) {
    
    private val playerDAO = PlayerDAO(plugin)
    private val cache = ConcurrentHashMap<UUID, CachedPlayerData>()
    private val pendingSaves = ConcurrentHashMap<UUID, PlayerData>()
    
    private data class CachedPlayerData(
        val data: PlayerData,
        val cachedAt: Long = System.currentTimeMillis()
    ) {
        fun isExpired(durationSeconds: Int): Boolean {
            return System.currentTimeMillis() - cachedAt > durationSeconds * 1000L
        }
    }
    
    init {
        // 启动定期保存任务
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, Runnable {
            flushPendingSaves()
            cleanExpiredCache()
        }, 20L * 60, 20L * 60) // 每分钟执行一次
    }
    
    fun getPlayerData(uuid: UUID): PlayerData? {
        // 先检查缓存
        val cached = cache[uuid]
        if (cached != null && !cached.isExpired(plugin.configManager.getCacheDuration())) {
            return cached.data
        }
        
        // 从数据库加载
        val data = playerDAO.getPlayerData(uuid)
        if (data != null) {
            cache[uuid] = CachedPlayerData(data)
        }
        return data
    }
    
    fun getPlayerDataAsync(uuid: UUID): CompletableFuture<PlayerData?> {
        // 先检查缓存
        val cached = cache[uuid]
        if (cached != null && !cached.isExpired(plugin.configManager.getCacheDuration())) {
            return CompletableFuture.completedFuture(cached.data)
        }
        
        return CompletableFuture.supplyAsync {
            val data = playerDAO.getPlayerData(uuid)
            if (data != null) {
                cache[uuid] = CachedPlayerData(data)
            }
            data
        }
    }
    
    fun getPlayerData(player: Player): PlayerData? = getPlayerData(player.uniqueId)
    
    fun getPlayerDataByName(name: String): PlayerData? {
        // 先检查在线玩家
        Bukkit.getPlayer(name)?.let { player ->
            return getPlayerData(player.uniqueId)
        }
        
        // 检查缓存
        cache.values.find { it.data.playerName.equals(name, ignoreCase = true) }?.let {
            return it.data
        }
        
        // 从数据库加载
        return playerDAO.getPlayerDataByName(name)
    }
    
    fun createOrLoadPlayerData(player: Player): PlayerData {
        val uuid = player.uniqueId
        
        // 检查缓存
        cache[uuid]?.let {
            it.data.playerName = player.name
            it.data.updateLastJoin()
            return it.data
        }
        
        // 从数据库加载或创建新数据
        var data = playerDAO.getPlayerData(uuid)
        if (data == null) {
            data = PlayerData.createNew(
                uuid = uuid,
                playerName = player.name,
                autoLock = plugin.configManager.isAutoLockNewPlayers()
            )
            savePlayerData(data)
            Logger.debug("为玩家 ${player.name} 创建新数据")
        } else {
            data.playerName = player.name
            data.updateLastJoin()
            scheduleAsyncSave(data)
        }
        
        cache[uuid] = CachedPlayerData(data)
        return data
    }
    
    fun savePlayerData(data: PlayerData): Boolean {
        cache[data.uuid] = CachedPlayerData(data)
        
        return if (plugin.configManager.isAsyncDatabase()) {
            scheduleAsyncSave(data)
            true
        } else {
            playerDAO.savePlayerData(data)
        }
    }
    
    private fun scheduleAsyncSave(data: PlayerData) {
        pendingSaves[data.uuid] = data
    }
    
    private fun flushPendingSaves() {
        if (pendingSaves.isEmpty()) return
        
        val toSave = pendingSaves.toMap()
        pendingSaves.clear()
        
        toSave.values.forEach { data ->
            try {
                playerDAO.savePlayerData(data)
            } catch (e: Exception) {
                Logger.error("异步保存玩家数据失败: ${data.uuid}", e)
                pendingSaves[data.uuid] = data // 重新加入队列
            }
        }
        
        if (toSave.isNotEmpty()) {
            Logger.debug("已保存 ${toSave.size} 条玩家数据")
        }
    }
    
    private fun cleanExpiredCache() {
        val duration = plugin.configManager.getCacheDuration()
        val maxSize = plugin.configManager.getMaxCacheSize()
        
        // 移除过期缓存
        cache.entries.removeIf { it.value.isExpired(duration) }
        
        // 如果缓存过大，移除最旧的数据
        if (cache.size > maxSize) {
            val sorted = cache.entries.sortedBy { it.value.cachedAt }
            val toRemove = sorted.take(cache.size - maxSize)
            toRemove.forEach { cache.remove(it.key) }
        }
    }
    
    fun setStarterLocked(uuid: UUID, locked: Boolean, lockedBy: UUID? = null, reason: String? = null): Boolean {
        val data = getPlayerData(uuid) ?: return false
        
        if (locked) {
            data.lock(lockedBy, reason)
        } else {
            data.unlock()
        }
        
        // 更新缓存
        cache[uuid] = CachedPlayerData(data)
        
        // 记录日志
        playerDAO.logAction(uuid, if (locked) "LOCK" else "UNLOCK", lockedBy, reason)
        
        // 保存到数据库
        return playerDAO.setStarterLocked(uuid, locked, lockedBy, reason)
    }
    
    fun markStarterSelected(uuid: UUID) {
        val data = getPlayerData(uuid) ?: return
        data.markStarterSelected()
        cache[uuid] = CachedPlayerData(data)
        scheduleAsyncSave(data)
        playerDAO.logAction(uuid, "STARTER_SELECTED", null, null)
    }
    
    fun isStarterLocked(uuid: UUID): Boolean {
        return getPlayerData(uuid)?.isLocked() ?: plugin.configManager.isAutoLockNewPlayers()
    }
    
    fun getAllPlayers(limit: Int = 1000, offset: Int = 0): List<PlayerData> {
        return playerDAO.getAllPlayers(limit, offset)
    }
    
    fun getPlayerCount(): Int = playerDAO.getPlayerCount()
    
    fun saveAllCache() {
        Logger.info("正在保存所有缓存数据...")
        flushPendingSaves()
        cache.values.forEach { cached ->
            try {
                playerDAO.savePlayerData(cached.data)
            } catch (e: Exception) {
                Logger.error("保存缓存数据失败: ${cached.data.uuid}", e)
            }
        }
        Logger.info("缓存数据保存完成")
    }
    
    fun invalidateCache(uuid: UUID) {
        cache.remove(uuid)
    }
    
    fun clearCache() {
        saveAllCache()
        cache.clear()
    }
}
