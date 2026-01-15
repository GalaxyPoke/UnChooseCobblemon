package com.unchoose.cobblemon.util

import net.md_5.bungee.api.ChatColor
import java.util.regex.Pattern

object ColorUtil {
    
    private val HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})")
    
    fun colorize(text: String): String {
        var result = text
        
        // 处理十六进制颜色 &#RRGGBB
        var matcher = HEX_PATTERN.matcher(result)
        while (matcher.find()) {
            val hexColor = matcher.group(1)
            val replacement = ChatColor.of("#$hexColor").toString()
            result = result.replace("&#$hexColor", replacement)
            matcher = HEX_PATTERN.matcher(result)
        }
        
        // 处理标准颜色代码 &X
        return ChatColor.translateAlternateColorCodes('&', result)
    }
    
    fun stripColor(text: String): String {
        return ChatColor.stripColor(colorize(text)) ?: text
    }
    
    fun List<String>.colorize(): List<String> = map { ColorUtil.colorize(it) }
}
