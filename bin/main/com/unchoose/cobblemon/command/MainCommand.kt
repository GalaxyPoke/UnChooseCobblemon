package com.unchoose.cobblemon.command

import com.unchoose.cobblemon.UnChooseCobblemon
import com.unchoose.cobblemon.gui.MainMenuGui
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class MainCommand(private val plugin: UnChooseCobblemon) : CommandExecutor, TabCompleter {
    
    private val config get() = plugin.configManager
    
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            showHelp(sender)
            return true
        }
        
        when (args[0].lowercase()) {
            "help", "?" -> showHelp(sender)
            "reload" -> handleReload(sender)
            "gui", "menu" -> handleGui(sender)
            "lock" -> handleLock(sender, args)
            "unlock" -> handleUnlock(sender, args)
            "status", "info" -> handleStatus(sender, args)
            "lockall" -> handleLockAll(sender)
            "unlockall" -> handleUnlockAll(sender)
            else -> {
                sender.sendMessage(config.getMessage("common.invalid-args", "usage" to "/uc help"))
            }
        }
        
        return true
    }
    
    private fun showHelp(sender: CommandSender) {
        val helpMessages = config.getMessageList("help.commands")
        sender.sendMessage(config.getRawMessage("help.header"))
        helpMessages.forEach { sender.sendMessage(it) }
        sender.sendMessage(config.getRawMessage("help.footer"))
    }
    
    private fun handleReload(sender: CommandSender) {
        if (!sender.hasPermission("unchoose.reload")) {
            sender.sendMessage(config.getMessage("common.no-permission"))
            return
        }
        
        if (plugin.reload()) {
            sender.sendMessage(config.getMessage("admin.reload-success"))
        } else {
            sender.sendMessage(config.getMessage("admin.reload-fail"))
        }
    }
    
    private fun handleGui(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage(config.getMessage("common.player-only"))
            return
        }
        
        if (!sender.hasPermission("unchoose.gui")) {
            sender.sendMessage(config.getMessage("common.no-permission"))
            return
        }
        
        if (!config.isGUIEnabled()) {
            sender.sendMessage(config.getMessage("gui.disabled"))
            return
        }
        
        MainMenuGui(plugin).open(sender)
    }
    
    private fun handleLock(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("unchoose.setstarterlock")) {
            sender.sendMessage(config.getMessage("common.no-permission"))
            return
        }
        
        if (args.size < 2) {
            sender.sendMessage(config.getMessage("common.invalid-args", "usage" to "/uc lock <玩家>"))
            return
        }
        
        val targetName = args[1]
        val targetData = plugin.playerDataManager.getPlayerDataByName(targetName)
        
        if (targetData == null) {
            sender.sendMessage(config.getMessage("common.player-not-found", "player" to targetName))
            return
        }
        
        val lockedBy = if (sender is Player) sender.uniqueId else null
        val reason = if (args.size > 2) args.drop(2).joinToString(" ") else null
        
        if (plugin.playerDataManager.setStarterLocked(targetData.uuid, true, lockedBy, reason)) {
            sender.sendMessage(config.getMessage("admin.lock-success", "player" to targetData.playerName))
            
            // 记录到文件日志
            plugin.fileLogger.logLock(
                targetData.uuid, 
                targetData.playerName, 
                lockedBy, 
                sender.name, 
                reason
            )
            
            // 如果玩家在线，立即同步到Cobblemon并通知玩家
            Bukkit.getPlayer(targetData.uuid)?.let { onlinePlayer ->
                plugin.cobblemonListener?.lockPlayer(onlinePlayer)
                onlinePlayer.sendMessage(config.getMessage("notify.locked"))
            }
        } else {
            sender.sendMessage(config.getMessage("database.error", "error" to "操作失败"))
        }
    }
    
    private fun handleUnlock(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("unchoose.setstarterlock")) {
            sender.sendMessage(config.getMessage("common.no-permission"))
            return
        }
        
        if (args.size < 2) {
            sender.sendMessage(config.getMessage("common.invalid-args", "usage" to "/uc unlock <玩家>"))
            return
        }
        
        val targetName = args[1]
        val targetData = plugin.playerDataManager.getPlayerDataByName(targetName)
        
        if (targetData == null) {
            sender.sendMessage(config.getMessage("common.player-not-found", "player" to targetName))
            return
        }
        
        val lockedBy = if (sender is Player) sender.uniqueId else null
        
        if (plugin.playerDataManager.setStarterLocked(targetData.uuid, false, lockedBy)) {
            sender.sendMessage(config.getMessage("admin.unlock-success", "player" to targetData.playerName))
            
            // 记录到文件日志
            plugin.fileLogger.logUnlock(
                targetData.uuid,
                targetData.playerName,
                lockedBy,
                sender.name
            )
            
            // 如果玩家在线，立即同步到Cobblemon并通知玩家
            Bukkit.getPlayer(targetData.uuid)?.let { onlinePlayer ->
                plugin.cobblemonListener?.unlockPlayer(onlinePlayer)
                onlinePlayer.sendMessage(config.getMessage("notify.unlocked"))
            }
        } else {
            sender.sendMessage(config.getMessage("database.error", "error" to "操作失败"))
        }
    }
    
    private fun handleStatus(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("unchoose.query")) {
            sender.sendMessage(config.getMessage("common.no-permission"))
            return
        }
        
        val targetName = if (args.size > 1) {
            args[1]
        } else if (sender is Player) {
            sender.name
        } else {
            sender.sendMessage(config.getMessage("common.invalid-args", "usage" to "/uc status <玩家>"))
            return
        }
        
        val targetData = plugin.playerDataManager.getPlayerDataByName(targetName)
        
        if (targetData == null) {
            sender.sendMessage(config.getMessage("common.player-not-found", "player" to targetName))
            return
        }
        
        sender.sendMessage(config.getMessage("status.query", "player" to targetData.playerName))
        sender.sendMessage(config.getRawMessage("status.query-locked",
            "status" to if (targetData.isLocked()) config.getRawMessage("status.locked") else config.getRawMessage("status.unlocked")
        ))
        sender.sendMessage(config.getRawMessage("status.query-selected",
            "status" to if (targetData.hasSelectedStarter()) "&b是" else "&7否"
        ))
        sender.sendMessage(config.getRawMessage("status.query-uuid", "uuid" to targetData.uuid.toString()))
    }
    
    private fun handleLockAll(sender: CommandSender) {
        if (!sender.hasPermission("unchoose.admin")) {
            sender.sendMessage(config.getMessage("common.no-permission"))
            return
        }
        
        val lockedBy = if (sender is Player) sender.uniqueId else null
        var count = 0
        
        Bukkit.getOnlinePlayers().forEach { player ->
            if (plugin.playerDataManager.setStarterLocked(player.uniqueId, true, lockedBy)) {
                count++
            }
        }
        
        sender.sendMessage(config.getMessage("admin.lock-success", "player" to "全部在线玩家($count)"))
    }
    
    private fun handleUnlockAll(sender: CommandSender) {
        if (!sender.hasPermission("unchoose.admin")) {
            sender.sendMessage(config.getMessage("common.no-permission"))
            return
        }
        
        val lockedBy = if (sender is Player) sender.uniqueId else null
        var count = 0
        
        Bukkit.getOnlinePlayers().forEach { player ->
            if (plugin.playerDataManager.setStarterLocked(player.uniqueId, false, lockedBy)) {
                count++
            }
        }
        
        sender.sendMessage(config.getMessage("admin.unlock-success", "player" to "全部在线玩家($count)"))
    }
    
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (args.isEmpty()) return emptyList()
        
        return when (args.size) {
            1 -> {
                val subCommands = mutableListOf("help", "reload", "gui", "lock", "unlock", "status", "lockall", "unlockall")
                subCommands.filter { it.startsWith(args[0].lowercase()) }
            }
            2 -> {
                when (args[0].lowercase()) {
                    "lock", "unlock", "status" -> {
                        Bukkit.getOnlinePlayers()
                            .map { it.name }
                            .filter { it.lowercase().startsWith(args[1].lowercase()) }
                    }
                    else -> emptyList()
                }
            }
            else -> emptyList()
        }
    }
}
