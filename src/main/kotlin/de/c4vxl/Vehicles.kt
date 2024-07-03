package de.c4vxl

import de.c4vxl.commands.SpawnVehicleCommand
import de.c4vxl.listeners.VehicleListeners
import de.c4vxl.vehicle.VehicleModels
import org.bukkit.Material
import org.bukkit.plugin.java.JavaPlugin

class Vehicles : JavaPlugin() {
    override fun onEnable() {
        logger.info("[+] Enabled ${pluginMeta.name} by c4vxl (Version: ${pluginMeta.version})") // log

        // register commands
        SpawnVehicleCommand(this)

        // register listeners
        VehicleListeners(this)

        VehicleModels.removeModel("name")
        VehicleModels.addModel("F1_PURPLE", 190, Material.DIAMOND)
    }

    override fun onDisable() {
        logger.info("[-] Disabled ${pluginMeta.name} by c4vxl") // log
    }
}