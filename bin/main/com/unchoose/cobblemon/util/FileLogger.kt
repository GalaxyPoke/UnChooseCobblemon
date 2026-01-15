package com.unchoose.cobblemon.util

import com.unchoose.cobblemon.UnChooseCobblemon
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import org.bukkit.Bukkit

/**
 * 文件日志记录器
 * 将操作日志记录到文件中
 */
class FileLogger(private val plugin: UnChooseCobblemon) {
    
    private val logQueue = ConcurrentLinkedQueue<String>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    private val fileDateFormat = SimpleDateFormat("yyyy-MM-dd")
    private val logsFolder: File
    
    init {
        logsFolder = File(plugin.dataFolder, "logs")
        if (!logsFolder.exists()) {
            logsFolder.mkdirs()
        }
        
        // 启动异步写入任务
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, Runnable {
            flushLogs()
        }, 20L * 30, 20L * 30) // 每30秒写入一次
    }
    
    /**
     * 记录锁定操作
     */
    fun logLock(targetUuid: UUID, targetName: String, operatorUuid: UUID?, operatorName: String, reason: String?) {
        val message = buildString {
            append("[LOCK] ")
            append("目标: $targetName ($targetUuid) | ")
            append("操作者: $operatorName")
            if (operatorUuid != null) append(" ($operatorUuid)")
            if (reason != null) append(" | 原因: $reason")
        }
        log(message)
    }
    
    /**
     * 记录解锁操作
     */
    fun logUnlock(targetUuid: UUID, targetName: String, operatorUuid: UUID?, operatorName: String) {
        val message = buildString {
            append("[UNLOCK] ")
            append("目标: $targetName ($targetUuid) | ")
            append("操作者: $operatorName")
            if (operatorUuid != null) append(" ($operatorUuid)")
        }
        log(message)
    }
    
    /**
     * 记录选择初始宝可梦
     */
    fun logStarterSelected(uuid: UUID, playerName: String) {
        val message = "[STARTER_SELECTED] 玩家: $playerName ($uuid) 选择了初始宝可梦"
        log(message)
    }
    
    /**
     * 记录玩家加入
     */
    fun logPlayerJoin(uuid: UUID, playerName: String, isLocked: Boolean) {
        val message = "[JOIN] 玩家: $playerName ($uuid) | 锁定状态: ${if (isLocked) "已锁定" else "未锁定"}"
        log(message)
    }
    
    /**
     * 记录配置重载
     */
    fun logReload(operatorName: String) {
        val message = "[RELOAD] 操作者: $operatorName 重载了配置"
        log(message)
    }
    
    /**
     * 记录通用消息
     */
    fun log(message: String) {
        val timestamp = dateFormat.format(Date())
        logQueue.offer("[$timestamp] $message")
    }
    
    /**
     * 将队列中的日志写入文件
     */
    private fun flushLogs() {
        if (logQueue.isEmpty()) return
        
        val logFile = File(logsFolder, "${fileDateFormat.format(Date())}.log")
        
        try {
            PrintWriter(FileWriter(logFile, true)).use { writer ->
                var entry = logQueue.poll()
                while (entry != null) {
                    writer.println(entry)
                    entry = logQueue.poll()
                }
            }
        } catch (e: Exception) {
            Logger.error("写入日志文件失败", e)
        }
    }
    
    /**
     * 立即写入所有日志（用于插件关闭时）
     */
    fun flush() {
        flushLogs()
    }
}
