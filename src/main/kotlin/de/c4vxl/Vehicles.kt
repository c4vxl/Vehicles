package de.c4vxl

import de.c4vxl.commands.SpawnVehicleCommand
import de.c4vxl.listeners.VehicleListeners
import org.bukkit.plugin.java.JavaPlugin

class Vehicles : JavaPlugin() {
    override fun onEnable() {
        logger.info("[+] Enabled ${pluginMeta.name} by c4vxl (Version: ${pluginMeta.version})") // log

        // register commands
        SpawnVehicleCommand(this)

        // register listeners
        VehicleListeners(this)
    }

    override fun onDisable() {
        logger.info("[-] Disabled ${pluginMeta.name} by c4vxl") // log
    }
}