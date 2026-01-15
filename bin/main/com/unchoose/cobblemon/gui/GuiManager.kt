package com.unchoose.cobblemon.gui

import com.unchoose.cobblemon.UnChooseCobblemon
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class GuiManager(private val plugin: UnChooseCobblemon) : Listener {
    
    private val openGuis = ConcurrentHashMap<UUID, AbstractGui>()
    
    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }
    
    fun openGui(player: Player, gui: AbstractGui) {
        openGuis[player.uniqueId] = gui
        gui.open(player)
    }
    
    fun closeGui(player: Player) {
        openGuis.remove(player.uniqueId)
        player.closeInventory()
    }
    
    fun getOpenGui(player: Player): AbstractGui? = openGuis[player.uniqueId]
    
    fun isGuiOpen(player: Player): Boolean = openGuis.containsKey(player.uniqueId)
    
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val gui = openGuis[player.uniqueId] ?: return
        
        event.isCancelled = true
        
        val slot = event.rawSlot
        if (slot < 0 || slot >= event.inventory.size) return
        
        gui.handleClick(player, slot, event.isShiftClick, event.isRightClick)
    }
    
    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        val gui = openGuis.remove(player.uniqueId) ?: return
        gui.onClose(player)
    }
    
    fun closeAll() {
        openGuis.keys.toList().forEach { uuid ->
            plugin.server.getPlayer(uuid)?.closeInventory()
        }
        openGuis.clear()
    }
}
