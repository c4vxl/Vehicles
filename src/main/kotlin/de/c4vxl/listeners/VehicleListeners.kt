package de.c4vxl.listeners

import de.c4vxl.vehicle.VehicleEntity
import de.c4vxl.vehicle.VehicleEntity.Companion.asVehicleEntity
import de.c4vxl.vehicle.VehicleEntity.Companion.isOnVehicleEntity
import de.c4vxl.vehicle.VehicleEntity.Companion.namespacedKey
import de.c4vxl.vehicle.VehicleEntity.Companion.vehicleEntity
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDismountEvent
import org.bukkit.event.entity.EntityMountEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin

class VehicleListeners(plugin: JavaPlugin): Listener {
    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    val vehicleDriverInventoryBackup: MutableMap<Player, Array<ItemStack?>> = mutableMapOf()

    @EventHandler
    fun onVehicleMount(event: EntityMountEvent) {
        val vehicle: VehicleEntity = event.mount.asVehicleEntity ?: return
        val player: Player = event.entity as? Player ?: return
        val speedOptions: MutableList<Double> = vehicle.displayItem.persistentDataContainer.get(namespacedKey("speed_controls"), PersistentDataType.STRING)?.split(",", ", ")?.mapNotNull { it.toDoubleOrNull() }?.toMutableList() ?: mutableListOf()

        // save current inventory
        vehicleDriverInventoryBackup[player] = player.inventory.contents

        // clear inventory
        player.inventory.clear()

        // add control items
        for (i in 1..speedOptions.size) {
            val speed: Double = speedOptions[i - 1]

            player.inventory.addItem(ItemStack(Material.LEATHER).apply {
                this.editMeta {
                    it.displayName(Component.text()
                        .append(Component.text("Gas - Level ")
                            .decorate(TextDecoration.BOLD)
                            .color(NamedTextColor.GRAY))
                        .append(Component.text(i)
                            .decorate(TextDecoration.BOLD)
                            .color(NamedTextColor.GRAY))
                        .append(Component.text(" | Right click (Hold)")
                            .color(NamedTextColor.DARK_GRAY))
                        .build())

                    it.lore(mutableListOf(
                        Component.text("Speed Factor:").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
                        Component.text(speed).color(NamedTextColor.GRAY),
                    ))

                    it.persistentDataContainer.set(namespacedKey("speed_lvl"), PersistentDataType.DOUBLE, speed)
                }
            })
        }
    }

    @EventHandler
    fun onVehicleDismount(event: EntityDismountEvent) {
        val vehicle: VehicleEntity = event.dismounted.asVehicleEntity ?: return
        val player: Player = event.entity as? Player ?: return

        // load saved inventory
        val inventoryContents: Array<ItemStack?> = vehicleDriverInventoryBackup[player] ?: arrayOf()

        // set inventory
        player.inventory.clear()
        player.inventory.contents = inventoryContents
    }

    @EventHandler
    fun onVehicleInteract(event: PlayerInteractAtEntityEvent) {
        val player: Player = event.player
        val vehicle: VehicleEntity = event.rightClicked.asVehicleEntity ?: return

        // return if player is driving
        if (player.isOnVehicleEntity) return

        // return if player is sneaking
        if (player.isSneaking) {
            player.inventory.addItem(vehicle.displayItem.apply {
                this.editMeta {
                    it.displayName(
                        Component.text("Right click to spawn the vehicle")
                            .color(NamedTextColor.GOLD)
                            .decorate(TextDecoration.BOLD)
                    )
                }
            })

            vehicle.remove()
        }
        else vehicle.mountDriver(player)

        event.isCancelled = true
    }

    @EventHandler
    fun onControlInteract(event: PlayerInteractEvent) {
        if (!event.action.isRightClick) return

        val player: Player = event.player
        val vehicle: VehicleEntity = player.vehicleEntity ?: return
        val item: ItemStack = event.item ?: return
        val speedLevel: Double = item.persistentDataContainer.get(namespacedKey("speed_lvl"), PersistentDataType.DOUBLE) ?: return

        vehicle.displayItemHolder.setRotation(event.player.yaw, vehicle.displayItemHolder.pitch) // align player's x-rotation

        vehicle.driveTick(speedLevel)

        event.isCancelled = true
    }

    @EventHandler
    fun onVehicleSpawnItem(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return // return if not right-click block

        val item: ItemStack = event.item ?: return
        val spawnLocation: Location = event.clickedBlock?.location?.add(0.0, 1.0, 0.0) ?: return

        // return if item is not a vehicle spawn item
        val vehicle: VehicleEntity = VehicleEntity.formItem(item.clone()) ?: return

        // spawn vehicle
        vehicle.spawn(spawnLocation)?.apply {
            this.displayItem = item.clone() // clone custom item properties
            this.displayItemHolder.setRotation(event.player.yaw, this.displayItemHolder.pitch) // align player's x-rotation
        }

        if (event.player.gameMode != GameMode.CREATIVE) item.amount -= 1

        event.isCancelled = true
    }

    @EventHandler
    fun onPlayerLogOut(event: PlayerQuitEvent) {
        val player: Player = event.player
        val vehicle: VehicleEntity = player.vehicleEntity ?: return

        // dismount
        vehicle.dismountDriver()
    }

    @EventHandler
    fun beforeDisable(event: PluginDisableEvent) {
        Bukkit.getOnlinePlayers().forEach { it.leaveVehicle() } // force all players to dismount their vehicles
    }
}