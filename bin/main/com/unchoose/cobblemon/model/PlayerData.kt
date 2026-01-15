package com.unchoose.cobblemon.model

import java.util.UUID

data class PlayerData(
    val uuid: UUID,
    var playerName: String,
    var starterLocked: Boolean = true,
    var starterSelected: Boolean = false,
    var lockReason: String? = null,
    var lockedBy: UUID? = null,
    var lockedAt: Long? = null,
    val firstJoin: Long = System.currentTimeMillis(),
    var lastJoin: Long = System.currentTimeMillis()
) {
    
    fun isLocked(): Boolean = starterLocked
    
    fun hasSelectedStarter(): Boolean = starterSelected
    
    fun lock(by: UUID? = null, reason: String? = null) {
        starterLocked = true
        lockedBy = by
        lockReason = reason
        lockedAt = System.currentTimeMillis()
    }
    
    fun unlock() {
        starterLocked = false
        lockedBy = null
        lockReason = null
        lockedAt = null
    }
    
    fun markStarterSelected() {
        starterSelected = true
    }
    
    fun updateLastJoin() {
        lastJoin = System.currentTimeMillis()
    }
    
    companion object {
        fun createNew(uuid: UUID, playerName: String, autoLock: Boolean = true): PlayerData {
            val now = System.currentTimeMillis()
            return PlayerData(
                uuid = uuid,
                playerName = playerName,
                starterLocked = autoLock,
                starterSelected = false,
                firstJoin = now,
                lastJoin = now
            )
        }
    }
}
