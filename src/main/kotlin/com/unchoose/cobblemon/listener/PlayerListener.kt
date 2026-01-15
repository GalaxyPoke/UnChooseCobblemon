package com.unchoose.cobblemon.listener

import com.unchoose.cobblemon.UnChooseCobblemon
import com.unchoose.cobblemon.util.Logger
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerListener(private val plugin: UnChooseCobblemon) : Listener {
    
    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        
        // 异步加载/创建玩家数据
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            try {
                plugin.playerDataManager.createOrLoadPlayerData(player)
                Logger.debug("已加载玩家 ${player.name} 的数据")
            } catch (e: Exception) {
                Logger.error("加载玩家数据失败: ${player.name}", e)
            }
        })
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        
        // 异步保存玩家数据
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            try {
                plugin.playerDataManager.getPlayerData(player)?.let { data ->
                    plugin.playerDataManager.savePlayerData(data)
                }
                Logger.debug("已保存玩家 ${player.name} 的数据")
            } catch (e: Exception) {
                Logger.error("保存玩家数据失败: ${player.name}", e)
            }
        })
    }
}
