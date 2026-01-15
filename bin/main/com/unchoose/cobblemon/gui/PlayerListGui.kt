package com.unchoose.cobblemon.gui

import com.unchoose.cobblemon.UnChooseCobblemon
import com.unchoose.cobblemon.model.PlayerData
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta

class PlayerListGui(
    plugin: UnChooseCobblemon,
    private var currentPage: Int = 1
) : AbstractGui(
    plugin,
    plugin.configManager.getPlayerMenuTitle().replace("{page}", currentPage.toString()),
    54
) {
    
    private val playersPerPage = 45 // 5行 x 9列
    private var playerList: List<PlayerData> = emptyList()
    private var totalPages = 1
    
    override fun setup() {
        // 加载玩家数据
        loadPlayerData()
        
        // 清空界面
        for (i in 0 until size) {
            setItem(i, null)
        }
        
        // 填充玩家头颅
        val startIndex = (currentPage - 1) * playersPerPage
        val endIndex = minOf(startIndex + playersPerPage, playerList.size)
        
        for (i in startIndex until endIndex) {
            val data = playerList[i]
            val slot = i - startIndex
            setItem(slot, createPlayerHead(data))
        }
        
        // 底部导航栏
        setupNavigation()
    }
    
    private fun loadPlayerData() {
        playerList = plugin.playerDataManager.getAllPlayers(1000, 0)
        totalPages = maxOf(1, (playerList.size + playersPerPage - 1) / playersPerPage)
        
        if (currentPage > totalPages) {
            currentPage = totalPages
        }
    }
    
    private fun createPlayerHead(data: PlayerData): ItemStack {
        val head = ItemStack(Material.PLAYER_HEAD)
        val meta = head.itemMeta as? SkullMeta ?: return head
        
        // 设置头颅所有者
        val offlinePlayer = Bukkit.getOfflinePlayer(data.uuid)
        meta.owningPlayer = offlinePlayer
        
        // 设置名称
        val statusColor = if (data.isLocked()) "&c" else "&a"
        val statusText = if (data.isLocked()) "[已锁定]" else "[已解锁]"
        meta.setDisplayName(com.unchoose.cobblemon.util.ColorUtil.colorize("$statusColor${data.playerName} $statusText"))
        
        // 设置描述
        val lore = mutableListOf<String>()
        lore.add("&7点击切换锁定状态")
        lore.add("&7Shift+点击查看详情")
        lore.add("")
        lore.add("&7UUID: &f${data.uuid}")
        lore.add("&7状态: ${if (data.isLocked()) "&c已锁定" else "&a已解锁"}")
        lore.add("&7已选初始: ${if (data.hasSelectedStarter()) "&b是" else "&7否"}")
        
        if (data.lockReason != null) {
            lore.add("&7锁定原因: &f${data.lockReason}")
        }
        
        meta.lore = lore.map { com.unchoose.cobblemon.util.ColorUtil.colorize(it) }
        head.itemMeta = meta
        
        return head
    }
    
    private fun setupNavigation() {
        // 填充底部背景
        for (i in 45 until 54) {
            setItem(i, getFiller())
        }
        
        // 返回按钮
        setItem(45, createItem(Material.ARROW, "&c返回主菜单"))
        
        // 上一页
        if (currentPage > 1) {
            setItem(48, createItem(
                Material.SPECTRAL_ARROW,
                "&a上一页",
                listOf("&7第 ${currentPage - 1} 页")
            ))
        }
        
        // 页码信息
        setItem(49, createItem(
            Material.PAPER,
            "&e第 $currentPage / $totalPages 页",
            listOf(
                "&7共 ${playerList.size} 名玩家",
                "",
                "&7点击刷新列表"
            )
        ))
        
        // 下一页
        if (currentPage < totalPages) {
            setItem(50, createItem(
                Material.SPECTRAL_ARROW,
                "&a下一页",
                listOf("&7第 ${currentPage + 1} 页")
            ))
        }
        
        // 关闭按钮
        setItem(53, createButton("close"))
    }
    
    override fun handleClick(player: Player, slot: Int, isShiftClick: Boolean, isRightClick: Boolean) {
        when {
            // 玩家头颅区域
            slot < 45 -> {
                val playerIndex = (currentPage - 1) * playersPerPage + slot
                if (playerIndex < playerList.size) {
                    val targetData = playerList[playerIndex]
                    
                    if (isShiftClick) {
                        // 查看详情
                        player.sendMessage(config.getMessage("status.query", "player" to targetData.playerName))
                        player.sendMessage(config.getRawMessage("status.query-locked", 
                            "status" to if (targetData.isLocked()) config.getRawMessage("status.locked") else config.getRawMessage("status.unlocked")
                        ))
                        player.sendMessage(config.getRawMessage("status.query-selected",
                            "status" to if (targetData.hasSelectedStarter()) "&b是" else "&7否"
                        ))
                        player.sendMessage(config.getRawMessage("status.query-uuid", "uuid" to targetData.uuid.toString()))
                    } else {
                        // 切换锁定状态
                        val newLocked = !targetData.isLocked()
                        if (plugin.playerDataManager.setStarterLocked(targetData.uuid, newLocked, player.uniqueId)) {
                            val message = if (newLocked) {
                                config.getMessage("admin.lock-success", "player" to targetData.playerName)
                            } else {
                                config.getMessage("admin.unlock-success", "player" to targetData.playerName)
                            }
                            player.sendMessage(message)
                            
                            // 刷新界面
                            plugin.server.scheduler.runTask(plugin, Runnable {
                                refresh(player)
                            })
                        }
                    }
                }
            }
            
            // 返回按钮
            slot == 45 -> {
                plugin.server.scheduler.runTask(plugin, Runnable {
                    MainMenuGui(plugin).open(player)
                })
            }
            
            // 上一页
            slot == 48 && currentPage > 1 -> {
                currentPage--
                refresh(player)
            }
            
            // 刷新
            slot == 49 -> {
                refresh(player)
            }
            
            // 下一页
            slot == 50 && currentPage < totalPages -> {
                currentPage++
                refresh(player)
            }
            
            // 关闭
            slot == 53 -> {
                player.closeInventory()
            }
        }
    }
    
    override fun refresh(player: Player) {
        loadPlayerData()
        super.refresh(player)
    }
}
