package com.unchoose.cobblemon.gui

import com.unchoose.cobblemon.UnChooseCobblemon
import org.bukkit.Material
import org.bukkit.entity.Player

class MainMenuGui(plugin: UnChooseCobblemon) : AbstractGui(
    plugin,
    plugin.configManager.getMainMenuTitle(),
    27
) {
    
    override fun setup() {
        // 填充背景
        if (config.isFillerEnabled()) {
            for (i in 0 until size) {
                setItem(i, getFiller())
            }
        }
        
        // 玩家管理按钮
        setItem(11, createItem(
            Material.PLAYER_HEAD,
            "&b&l玩家管理",
            listOf(
                "&7管理玩家的初始宝可梦选择权限",
                "",
                "&e点击打开玩家列表"
            )
        ))
        
        // 全局操作按钮
        setItem(13, createItem(
            Material.COMMAND_BLOCK,
            "&6&l全局操作",
            listOf(
                "&7批量管理所有玩家",
                "",
                "&a左键: 解锁所有在线玩家",
                "&c右键: 锁定所有在线玩家"
            )
        ))
        
        // 统计信息
        val playerCount = plugin.playerDataManager.getPlayerCount()
        setItem(15, createItem(
            Material.BOOK,
            "&e&l统计信息",
            listOf(
                "&7插件运行状态",
                "",
                "&7总玩家数: &f$playerCount",
                "&7在线玩家: &f${plugin.server.onlinePlayers.size}",
                "&7数据库类型: &f${config.getDatabaseType()}"
            )
        ))
        
        // 关闭按钮
        setItem(22, createButton("close"))
    }
    
    override fun handleClick(player: Player, slot: Int, isShiftClick: Boolean, isRightClick: Boolean) {
        when (slot) {
            11 -> {
                // 打开玩家管理界面
                plugin.server.scheduler.runTask(plugin, Runnable {
                    PlayerListGui(plugin, 1).open(player)
                })
            }
            13 -> {
                // 全局操作
                if (isRightClick) {
                    // 锁定所有在线玩家
                    var count = 0
                    plugin.server.onlinePlayers.forEach { target ->
                        if (plugin.playerDataManager.setStarterLocked(target.uniqueId, true, player.uniqueId)) {
                            count++
                        }
                    }
                    player.sendMessage(config.getMessage("admin.lock-success", "player" to "全部在线玩家($count)"))
                } else {
                    // 解锁所有在线玩家
                    var count = 0
                    plugin.server.onlinePlayers.forEach { target ->
                        if (plugin.playerDataManager.setStarterLocked(target.uniqueId, false, player.uniqueId)) {
                            count++
                        }
                    }
                    player.sendMessage(config.getMessage("admin.unlock-success", "player" to "全部在线玩家($count)"))
                }
                refresh(player)
            }
            22 -> {
                player.closeInventory()
            }
        }
    }
}
