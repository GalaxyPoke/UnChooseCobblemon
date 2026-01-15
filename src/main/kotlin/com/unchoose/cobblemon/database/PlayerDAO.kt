package com.unchoose.cobblemon.database

import com.unchoose.cobblemon.UnChooseCobblemon
import com.unchoose.cobblemon.model.PlayerData
import com.unchoose.cobblemon.util.Logger
import java.sql.ResultSet
import java.util.UUID
import java.util.concurrent.CompletableFuture

class PlayerDAO(private val plugin: UnChooseCobblemon) {
    
    private val dbManager get() = plugin.databaseManager
    
    fun getPlayerData(uuid: UUID): PlayerData? {
        return try {
            dbManager.getConnection()?.use { conn ->
                conn.prepareStatement("SELECT * FROM player_data WHERE uuid = ?").use { stmt ->
                    stmt.setString(1, uuid.toString())
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) mapResultSet(rs) else null
                    }
                }
            }
        } catch (e: Exception) {
            Logger.error("获取玩家数据失败: $uuid", e)
            null
        }
    }
    
    fun getPlayerDataAsync(uuid: UUID): CompletableFuture<PlayerData?> {
        return CompletableFuture.supplyAsync { getPlayerData(uuid) }
    }
    
    fun getPlayerDataByName(name: String): PlayerData? {
        return try {
            dbManager.getConnection()?.use { conn ->
                conn.prepareStatement("SELECT * FROM player_data WHERE LOWER(player_name) = LOWER(?)").use { stmt ->
                    stmt.setString(1, name)
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) mapResultSet(rs) else null
                    }
                }
            }
        } catch (e: Exception) {
            Logger.error("获取玩家数据失败: $name", e)
            null
        }
    }
    
    fun savePlayerData(data: PlayerData): Boolean {
        return try {
            dbManager.getConnection()?.use { conn ->
                conn.prepareStatement("""
                    INSERT OR REPLACE INTO player_data 
                    (uuid, player_name, starter_locked, starter_selected, lock_reason, locked_by, locked_at, first_join, last_join, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()).use { stmt ->
                    stmt.setString(1, data.uuid.toString())
                    stmt.setString(2, data.playerName)
                    stmt.setBoolean(3, data.starterLocked)
                    stmt.setBoolean(4, data.starterSelected)
                    stmt.setString(5, data.lockReason)
                    stmt.setString(6, data.lockedBy?.toString())
                    stmt.setLong(7, data.lockedAt ?: 0)
                    stmt.setLong(8, data.firstJoin)
                    stmt.setLong(9, data.lastJoin)
                    stmt.setLong(10, System.currentTimeMillis())
                    stmt.executeUpdate() > 0
                }
            } ?: false
        } catch (e: Exception) {
            // 尝试MySQL语法
            try {
                dbManager.getConnection()?.use { conn ->
                    conn.prepareStatement("""
                        INSERT INTO player_data 
                        (uuid, player_name, starter_locked, starter_selected, lock_reason, locked_by, locked_at, first_join, last_join, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE 
                        player_name = VALUES(player_name),
                        starter_locked = VALUES(starter_locked),
                        starter_selected = VALUES(starter_selected),
                        lock_reason = VALUES(lock_reason),
                        locked_by = VALUES(locked_by),
                        locked_at = VALUES(locked_at),
                        last_join = VALUES(last_join),
                        updated_at = VALUES(updated_at)
                    """.trimIndent()).use { stmt ->
                        stmt.setString(1, data.uuid.toString())
                        stmt.setString(2, data.playerName)
                        stmt.setBoolean(3, data.starterLocked)
                        stmt.setBoolean(4, data.starterSelected)
                        stmt.setString(5, data.lockReason)
                        stmt.setString(6, data.lockedBy?.toString())
                        stmt.setLong(7, data.lockedAt ?: 0)
                        stmt.setLong(8, data.firstJoin)
                        stmt.setLong(9, data.lastJoin)
                        stmt.setLong(10, System.currentTimeMillis())
                        stmt.executeUpdate() > 0
                    }
                } ?: false
            } catch (e2: Exception) {
                Logger.error("保存玩家数据失败: ${data.uuid}", e2)
                false
            }
        }
    }
    
    fun savePlayerDataAsync(data: PlayerData): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync { savePlayerData(data) }
    }
    
    fun getAllPlayers(limit: Int = 1000, offset: Int = 0): List<PlayerData> {
        return try {
            dbManager.getConnection()?.use { conn ->
                conn.prepareStatement("SELECT * FROM player_data ORDER BY last_join DESC LIMIT ? OFFSET ?").use { stmt ->
                    stmt.setInt(1, limit)
                    stmt.setInt(2, offset)
                    stmt.executeQuery().use { rs ->
                        val list = mutableListOf<PlayerData>()
                        while (rs.next()) {
                            list.add(mapResultSet(rs))
                        }
                        list
                    }
                }
            } ?: emptyList()
        } catch (e: Exception) {
            Logger.error("获取所有玩家数据失败", e)
            emptyList()
        }
    }
    
    fun getPlayerCount(): Int {
        return try {
            dbManager.getConnection()?.use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeQuery("SELECT COUNT(*) FROM player_data").use { rs ->
                        if (rs.next()) rs.getInt(1) else 0
                    }
                }
            } ?: 0
        } catch (e: Exception) {
            Logger.error("获取玩家数量失败", e)
            0
        }
    }
    
    fun setStarterLocked(uuid: UUID, locked: Boolean, lockedBy: UUID? = null, reason: String? = null): Boolean {
        return try {
            dbManager.getConnection()?.use { conn ->
                conn.prepareStatement("""
                    UPDATE player_data 
                    SET starter_locked = ?, locked_by = ?, lock_reason = ?, locked_at = ?, updated_at = ?
                    WHERE uuid = ?
                """.trimIndent()).use { stmt ->
                    stmt.setBoolean(1, locked)
                    stmt.setString(2, lockedBy?.toString())
                    stmt.setString(3, reason)
                    stmt.setLong(4, if (locked) System.currentTimeMillis() else 0)
                    stmt.setLong(5, System.currentTimeMillis())
                    stmt.setString(6, uuid.toString())
                    stmt.executeUpdate() > 0
                }
            } ?: false
        } catch (e: Exception) {
            Logger.error("更新玩家锁定状态失败: $uuid", e)
            false
        }
    }
    
    fun logAction(playerUuid: UUID, actionType: String, actionBy: UUID?, actionData: String?) {
        try {
            dbManager.getConnection()?.use { conn ->
                conn.prepareStatement("""
                    INSERT INTO action_log (player_uuid, action_type, action_by, action_data, timestamp)
                    VALUES (?, ?, ?, ?, ?)
                """.trimIndent()).use { stmt ->
                    stmt.setString(1, playerUuid.toString())
                    stmt.setString(2, actionType)
                    stmt.setString(3, actionBy?.toString())
                    stmt.setString(4, actionData)
                    stmt.setLong(5, System.currentTimeMillis())
                    stmt.executeUpdate()
                }
            }
        } catch (e: Exception) {
            Logger.error("记录操作日志失败", e)
        }
    }
    
    private fun mapResultSet(rs: ResultSet): PlayerData {
        return PlayerData(
            uuid = UUID.fromString(rs.getString("uuid")),
            playerName = rs.getString("player_name"),
            starterLocked = rs.getBoolean("starter_locked"),
            starterSelected = rs.getBoolean("starter_selected"),
            lockReason = rs.getString("lock_reason"),
            lockedBy = rs.getString("locked_by")?.let { UUID.fromString(it) },
            lockedAt = rs.getLong("locked_at").takeIf { it > 0 },
            firstJoin = rs.getLong("first_join"),
            lastJoin = rs.getLong("last_join")
        )
    }
}
