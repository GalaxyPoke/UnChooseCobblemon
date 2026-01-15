package com.unchoose.cobblemon.gui

import com.unchoose.cobblemon.UnChooseCobblemon
import com.unchoose.cobblemon.util.ColorUtil
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

abstract class AbstractGui(
    protected val plugin: UnChooseCobblemon,
    protected val title: String,
    protected val size: Int = 54
) {
    
    protected lateinit var inventory: Inventory
    protected val config get() = plugin.configManager
    
    abstract fun setup()
    abstract fun handleClick(player: Player, slot: Int, isShiftClick: Boolean, isRightClick: Boolean)
    
    open fun open(player: Player) {
        inventory = Bukkit.createInventory(null, size, ColorUtil.colorize(title))
        setup()
        player.openInventory(inventory)
    }
    
    open fun onClose(player: Player) {}
    
    open fun refresh(player: Player) {
        setup()
        player.updateInventory()
    }
    
    protected fun setItem(slot: Int, item: ItemStack?) {
        if (slot in 0 until size) {
            inventory.setItem(slot, item)
        }
    }
    
    protected fun fillBorder(material: Material = Material.GRAY_STAINED_GLASS_PANE, name: String = " ") {
        val filler = createItem(material, name)
        
        // 顶部和底部
        for (i in 0 until 9) {
            setItem(i, filler)
            setItem(size - 9 + i, filler)
        }
        
        // 左右两边
        for (i in 1 until (size / 9) - 1) {
            setItem(i * 9, filler)
            setItem(i * 9 + 8, filler)
        }
    }
    
    protected fun fillEmpty(material: Material = Material.GRAY_STAINED_GLASS_PANE, name: String = " ") {
        val filler = createItem(material, name)
        for (i in 0 until size) {
            if (inventory.getItem(i) == null) {
                setItem(i, filler)
            }
        }
    }
    
    protected fun createItem(
        material: Material,
        name: String,
        lore: List<String> = emptyList(),
        amount: Int = 1
    ): ItemStack {
        val item = ItemStack(material, amount)
        val meta = item.itemMeta ?: return item
        
        meta.setDisplayName(ColorUtil.colorize(name))
        if (lore.isNotEmpty()) {
            meta.lore = lore.map { ColorUtil.colorize(it) }
        }
        
        item.itemMeta = meta
        return item
    }
    
    protected fun createButton(buttonKey: String, extraLore: List<String> = emptyList()): ItemStack {
        val materialName = config.getButtonMaterial(buttonKey)
        val material = try {
            Material.valueOf(materialName.uppercase())
        } catch (e: Exception) {
            Material.STONE
        }
        
        val name = config.getButtonName(buttonKey)
        return createItem(material, name, extraLore)
    }
    
    protected fun getFiller(): ItemStack {
        val materialName = config.getFillerMaterial()
        val material = try {
            Material.valueOf(materialName.uppercase())
        } catch (e: Exception) {
            Material.GRAY_STAINED_GLASS_PANE
        }
        return createItem(material, config.getFillerName())
    }
}
