package com.unchoose.cobblemon.listener

import com.unchoose.cobblemon.UnChooseCobblemon
import com.unchoose.cobblemon.util.ColorUtil
import com.unchoose.cobblemon.util.Logger
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import java.lang.reflect.Proxy
import java.util.UUID

/**
 * Cobblemon事件监听器
 * 
 * 核心原理：
 * 1. 替换StarterHandler为空实现
 * 2. 订阅STARTER_CHOSEN事件并取消
 * 3. 标记玩家starterSelected=true，让Cobblemon认为已选过
 */
class CobblemonEventListener(private val plugin: UnChooseCobblemon) : Listener {
    
    private val config get() = plugin.configManager
    
    init {
        // 在初始化时注册Cobblemon事件
        if (config.isStarterBlockEnabled()) {
            registerDataSyncHandler()
            registerStarterChosenHandler()
            Logger.info("初始宝可梦选择控制已启用（针对锁定玩家）")
        }
    }
    
    /**
     * 注册STARTER_CHOSEN事件处理器 - 当玩家选择后更新我们的数据库
     */
    private fun registerStarterChosenHandler() {
        try {
            val eventsClass = Class.forName("com.cobblemon.mod.common.api.events.CobblemonEvents")
            val starterChosenField = eventsClass.getField("STARTER_CHOSEN")
            val starterChosenEvent = starterChosenField.get(null)
            
            val priorityClass = Class.forName("com.cobblemon.mod.common.api.Priority")
            val lowestPriority = priorityClass.getField("LOWEST").get(null)
            
            val subscribeMethod = starterChosenEvent.javaClass.methods.find { 
                it.name == "subscribe" && it.parameterCount == 2 
            }
            
            if (subscribeMethod != null) {
                val function1Class = Class.forName("kotlin.jvm.functions.Function1")
                val kotlinHandler = Proxy.newProxyInstance(
                    function1Class.classLoader,
                    arrayOf(function1Class)
                ) { _, method, args ->
                    if (method.name == "invoke") {
                        val event = args[0]
                        try {
                            // 获取玩家
                            val getPlayerMethod = event.javaClass.getMethod("getPlayer")
                            val mcPlayer = getPlayerMethod.invoke(event)
                            val uuid = getPlayerUUID(mcPlayer)
                            
                            if (uuid != null) {
                                // 更新我们的数据库
                                plugin.playerDataManager.markStarterSelected(uuid)
                                Logger.info("玩家 $uuid 已选择初始宝可梦，已更新数据库")
                            }
                        } catch (e: Exception) {
                            Logger.debug("处理STARTER_CHOSEN事件失败: ${e.message}")
                        }
                    }
                    Class.forName("kotlin.Unit").getField("INSTANCE").get(null)
                }
                
                subscribeMethod.invoke(starterChosenEvent, lowestPriority, kotlinHandler)
                Logger.info("已注册STARTER_CHOSEN事件处理器")
            }
        } catch (e: Exception) {
            Logger.warn("注册STARTER_CHOSEN事件失败: ${e.message}")
        }
    }
    
    /**
     * 注册数据同步处理器 - 在Cobblemon数据同步时检查锁定状态
     */
    private fun registerDataSyncHandler() {
        try {
            val eventsClass = Class.forName("com.cobblemon.mod.common.api.events.CobblemonEvents")
            val dataSyncField = eventsClass.getField("DATA_SYNCHRONIZED")
            val dataSyncEvent = dataSyncField.get(null)
            
            val priorityClass = Class.forName("com.cobblemon.mod.common.api.Priority")
            val highestPriority = priorityClass.getField("HIGHEST").get(null)
            
            val subscribeMethod = dataSyncEvent.javaClass.methods.find { 
                it.name == "subscribe" && it.parameterCount == 2 
            }
            
            if (subscribeMethod != null) {
                val function1Class = Class.forName("kotlin.jvm.functions.Function1")
                val kotlinHandler = Proxy.newProxyInstance(
                    function1Class.classLoader,
                    arrayOf(function1Class)
                ) { _, method, args ->
                    if (method.name == "invoke" && config.isStarterBlockEnabled()) {
                        val mcPlayer = args[0]
                        try {
                            handlePlayerDataSync(mcPlayer)
                        } catch (e: Exception) {
                            Logger.debug("数据同步处理失败: ${e.message}")
                        }
                    }
                    Class.forName("kotlin.Unit").getField("INSTANCE").get(null)
                }
                
                subscribeMethod.invoke(dataSyncEvent, highestPriority, kotlinHandler)
                Logger.info("已注册DATA_SYNCHRONIZED事件处理器")
            }
        } catch (e: Exception) {
            Logger.warn("注册DATA_SYNCHRONIZED事件失败: ${e.message}")
        }
    }
    
    /**
     * 处理玩家数据同步 - 根据锁定状态决定是否阻止选择
     */
    private fun handlePlayerDataSync(mcPlayer: Any) {
        // 获取玩家UUID
        val uuid = getPlayerUUID(mcPlayer) ?: return
        val bukkitPlayer = Bukkit.getPlayer(uuid)
        
        // 检查绕过权限
        if (bukkitPlayer != null && config.isAllowBypass() && bukkitPlayer.hasPermission("unchoose.bypass")) {
            Logger.debug("玩家 ${bukkitPlayer.name} 拥有绕过权限")
            return
        }
        
        // 检查是否锁定
        val isLocked = plugin.playerDataManager.isStarterLocked(uuid)
        val hasSelected = plugin.playerDataManager.getPlayerData(uuid)?.hasSelectedStarter() ?: false
        
        if (isLocked && !hasSelected) {
            // 锁定且未选择 -> 设置starterSelected=true阻止选择
            setCobblemonStarterSelected(mcPlayer, true)
            Logger.debug("玩家 $uuid 已锁定，阻止选择初始宝可梦")
        } else if (!isLocked && !hasSelected) {
            // 未锁定且未选择 -> 确保starterSelected=false允许选择
            setCobblemonStarterSelected(mcPlayer, false)
            Logger.debug("玩家 $uuid 未锁定，允许选择初始宝可梦")
        }
    }
    
    /**
     * 获取MC玩家的UUID
     */
    private fun getPlayerUUID(mcPlayer: Any): UUID? {
        try {
            val methods = arrayOf("getUUID", "getUuid", "method_5667", "m_5667")
            for (methodName in methods) {
                try {
                    val method = mcPlayer.javaClass.getMethod(methodName)
                    val result = method.invoke(mcPlayer)
                    if (result is UUID) return result
                } catch (e: NoSuchMethodException) { }
            }
            
            // 尝试通过GameProfile获取
            val getGameProfile = mcPlayer.javaClass.getMethod("getGameProfile")
            val gameProfile = getGameProfile.invoke(mcPlayer)
            val getId = gameProfile.javaClass.getMethod("getId")
            return getId.invoke(gameProfile) as? UUID
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * 设置Cobblemon的starterSelected状态
     */
    private fun setCobblemonStarterSelected(mcPlayer: Any, selected: Boolean) {
        try {
            val cobblemonClass = Class.forName("com.cobblemon.mod.common.Cobblemon")
            val instanceField = cobblemonClass.getField("INSTANCE")
            val cobblemonInstance = instanceField.get(null)
            
            val getPlayerDataManagerMethod = cobblemonClass.getMethod("getPlayerDataManager")
            val dataManager = getPlayerDataManagerMethod.invoke(cobblemonInstance)
            
            // 获取玩家数据
            var playerData: Any? = null
            for (m in dataManager.javaClass.methods) {
                if (m.name == "getGenericData" && m.parameterCount == 1) {
                    try {
                        playerData = m.invoke(dataManager, mcPlayer)
                        if (playerData != null) break
                    } catch (e: Exception) { }
                }
            }
            
            if (playerData != null) {
                // 设置starterSelected
                for (m in playerData.javaClass.methods) {
                    if (m.name == "setStarterSelected" && m.parameterCount == 1) {
                        m.invoke(playerData, selected)
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Logger.debug("setCobblemonStarterSelected失败: ${e.message}")
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        
        if (!config.isStarterBlockEnabled()) return
        
        // 检查绕过权限
        if (config.isAllowBypass() && player.hasPermission("unchoose.bypass")) {
            Logger.debug("玩家 ${player.name} 拥有绕过权限")
            return
        }
        
        // 确保玩家数据存在
        plugin.playerDataManager.createOrLoadPlayerData(player)
        
        // 延迟同步状态到Cobblemon
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            syncPlayerStatus(player)
        }, 40L)
    }
    
    /**
     * 同步玩家状态到Cobblemon
     */
    fun syncPlayerStatus(player: Player) {
        val uuid = player.uniqueId
        val isLocked = plugin.playerDataManager.isStarterLocked(uuid)
        val hasSelected = plugin.playerDataManager.getPlayerData(uuid)?.hasSelectedStarter() ?: false
        
        try {
            val mcPlayer = player.javaClass.getMethod("getHandle").invoke(player)
            
            if (isLocked && !hasSelected) {
                // 锁定且未选择 -> 阻止选择
                setCobblemonStarterSelected(mcPlayer, true)
                Logger.info("玩家 ${player.name} 已锁定，已阻止初始宝可梦选择")
            } else if (!isLocked && !hasSelected) {
                // 未锁定且未选择 -> 允许选择
                setCobblemonStarterSelected(mcPlayer, false)
                Logger.info("玩家 ${player.name} 未锁定，可以选择初始宝可梦")
            }
        } catch (e: Exception) {
            Logger.warn("同步玩家状态失败: ${e.message}")
        }
    }
    
    /**
     * 锁定玩家 - 阻止选择初始宝可梦
     */
    fun lockPlayer(player: Player) {
        val hasSelected = plugin.playerDataManager.getPlayerData(player.uniqueId)?.hasSelectedStarter() ?: false
        if (!hasSelected) {
            try {
                val mcPlayer = player.javaClass.getMethod("getHandle").invoke(player)
                setCobblemonStarterSelected(mcPlayer, true)
                Logger.info("已锁定玩家 ${player.name} 的初始宝可梦选择")
            } catch (e: Exception) {
                Logger.warn("锁定玩家失败: ${e.message}")
            }
        }
    }
    
    /**
     * 解锁玩家 - 允许选择初始宝可梦（如果还没选过）
     */
    fun unlockPlayer(player: Player) {
        val hasSelected = plugin.playerDataManager.getPlayerData(player.uniqueId)?.hasSelectedStarter() ?: false
        if (!hasSelected) {
            try {
                val mcPlayer = player.javaClass.getMethod("getHandle").invoke(player)
                setCobblemonStarterSelected(mcPlayer, false)
                
                // 触发选择界面
                requestStarterChoice(mcPlayer)
                
                Logger.info("已解锁玩家 ${player.name} 的初始宝可梦选择并打开选择界面")
            } catch (e: Exception) {
                Logger.warn("解锁玩家失败: ${e.message}")
            }
        } else {
            Logger.info("玩家 ${player.name} 已选择过初始宝可梦，解锁无效")
        }
    }
    
    /**
     * 请求打开初始宝可梦选择界面
     */
    fun requestStarterChoice(mcPlayer: Any) {
        try {
            val cobblemonClass = Class.forName("com.cobblemon.mod.common.Cobblemon")
            val instanceField = cobblemonClass.getField("INSTANCE")
            val cobblemonInstance = instanceField.get(null)
            
            // 获取starterHandler
            val getStarterHandlerMethod = cobblemonClass.getMethod("getStarterHandler")
            val starterHandler = getStarterHandlerMethod.invoke(cobblemonInstance)
            
            // 调用requestStarterChoice方法
            for (m in starterHandler.javaClass.methods) {
                if (m.name == "requestStarterChoice" && m.parameterCount == 1) {
                    m.invoke(starterHandler, mcPlayer)
                    Logger.debug("已请求打开初始宝可梦选择界面")
                    return
                }
            }
            Logger.warn("未找到requestStarterChoice方法")
        } catch (e: Exception) {
            Logger.warn("请求选择界面失败: ${e.message}")
        }
    }
    
    /**
     * 检查玩家是否可以选择初始宝可梦
     */
    fun canPlayerSelectStarter(player: Player): Boolean {
        // 检查是否启用了禁用功能
        if (!config.isStarterBlockEnabled()) {
            return true
        }
        
        // 检查绕过权限
        if (config.isAllowBypass() && player.hasPermission("unchoose.bypass")) {
            Logger.debug("玩家 ${player.name} 拥有绕过权限")
            return true
        }
        
        // 检查玩家锁定状态
        val isLocked = plugin.playerDataManager.isStarterLocked(player.uniqueId)
        
        if (isLocked) {
            handleBlockedSelection(player)
            logBlockedAttempt(player)
            return false
        }
        
        return true
    }
    
    /**
     * 处理被阻止的选择尝试
     */
    private fun handleBlockedSelection(player: Player) {
        when (config.getMessageType()) {
            "actionbar" -> {
                val message = config.getRawMessage("starter.blocked-actionbar")
                player.spigot().sendMessage(
                    ChatMessageType.ACTION_BAR,
                    TextComponent(message)
                )
            }
            "title" -> {
                val title = config.getRawMessage("starter.blocked-title")
                val subtitle = config.getRawMessage("starter.blocked-subtitle")
                player.sendTitle(title, subtitle, 10, 70, 20)
            }
            else -> {
                player.sendMessage(config.getMessage("starter.blocked"))
            }
        }
    }
    
    /**
     * 记录被阻止的选择尝试
     */
    private fun logBlockedAttempt(player: Player) {
        if (config.isLogAttempts()) {
            Logger.info("阻止了玩家 ${player.name} 的初始宝可梦选择尝试")
        }
    }
    
    /**
     * 当玩家成功选择初始宝可梦后调用
     */
    fun onStarterSelected(player: Player, pokemonName: String) {
        plugin.playerDataManager.markStarterSelected(player.uniqueId)
        Logger.debug("玩家 ${player.name} 已选择初始宝可梦: $pokemonName")
    }
    
    /**
     * 注册Cobblemon事件
     * 
     * 注意：此方法需要根据实际使用的混合服务端进行适配
     * 可能的实现方式：
     * 1. 使用反射调用Cobblemon的事件API
     * 2. 使用混合服务端提供的桥接API
     * 3. 通过ProtocolLib拦截数据包
     */
    fun registerCobblemonEvents() {
        try {
            // 尝试通过反射获取Cobblemon事件系统
            val cobblemonEventsClass = Class.forName("com.cobblemon.mod.common.api.events.CobblemonEvents")
            val starterChosenField = cobblemonEventsClass.getField("STARTER_CHOSEN")
            val starterChosenObservable = starterChosenField.get(null)
            
            // 获取subscribe方法
            val subscribeMethod = starterChosenObservable.javaClass.getMethod("subscribe", Any::class.java, Function1::class.java)
            
            // 创建事件处理器
            val handler: (Any) -> Unit = { event ->
                handleStarterChosenEvent(event)
            }
            
            subscribeMethod.invoke(starterChosenObservable, plugin, handler)
            Logger.info("已成功注册Cobblemon事件监听器")
            
        } catch (e: ClassNotFoundException) {
            Logger.warn("未找到Cobblemon类，事件监听器未注册")
            Logger.debug("请确保使用的是支持Cobblemon的混合服务端")
        } catch (e: Exception) {
            Logger.error("注册Cobblemon事件监听器失败", e)
        }
    }
    
    /**
     * 处理StarterChosenEvent
     */
    private fun handleStarterChosenEvent(event: Any) {
        try {
            // 通过反射获取事件属性
            val playerField = event.javaClass.getField("player")
            val serverPlayer = playerField.get(event)
            
            // 获取Bukkit玩家
            val getBukkitEntityMethod = serverPlayer.javaClass.getMethod("getBukkitEntity")
            val bukkitPlayer = getBukkitEntityMethod.invoke(serverPlayer) as? Player ?: return
            
            // 检查是否允许选择
            if (!canPlayerSelectStarter(bukkitPlayer)) {
                // 尝试取消事件
                try {
                    val cancelMethod = event.javaClass.getMethod("cancel")
                    cancelMethod.invoke(event)
                    Logger.debug("已取消玩家 ${bukkitPlayer.name} 的初始宝可梦选择事件")
                } catch (e: Exception) {
                    Logger.warn("无法取消事件: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Logger.error("处理StarterChosenEvent失败", e)
        }
    }
}
