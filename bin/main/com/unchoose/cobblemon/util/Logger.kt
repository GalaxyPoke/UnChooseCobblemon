package com.unchoose.cobblemon.util

import com.unchoose.cobblemon.UnChooseCobblemon
import java.util.logging.Level

object Logger {
    
    private val logger by lazy { UnChooseCobblemon.instance.logger }
    
    fun info(message: String) {
        logger.info(message)
    }
    
    fun warn(message: String) {
        logger.warning(message)
    }
    
    fun severe(message: String) {
        logger.severe(message)
    }
    
    fun debug(message: String) {
        if (UnChooseCobblemon.instance.configManager.isVerbose()) {
            logger.info("[DEBUG] $message")
        }
    }
    
    fun error(message: String, throwable: Throwable? = null) {
        logger.log(Level.SEVERE, message, throwable)
    }
}
