package de.c4vxl.commands

import de.c4vxl.vehicle.VehicleEntity
import de.c4vxl.vehicle.VehicleModel
import de.c4vxl.vehicle.VehicleModels
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

class SpawnVehicleCommand(plugin: JavaPlugin) {
    init {
        plugin.getCommand("spawnvehicle")?.let { cmd ->

            cmd.setExecutor { sender, command, label, args ->
                val player: Player = (sender as? Player).let {
                    if (it == null) {
                        sender.sendMessage("§c§lI am sorry! §r§cBut you have to be a player to perform this command!")
                        return@setExecutor true
                    }

                    it
                }

                if (args.getOrNull(0).contentEquals("help")) return@setExecutor false

                val location: Location = player.location.apply { this.pitch = 3.0f }

                val isSmall: Boolean = args.getOrNull(0).contentEquals("small")
                val controlSpeeds: String = args.getOrElse(1) { "0.01,0.2,2" }
                val gravityFactor: Double = args.getOrElse(2) { "gravityFactor:1" }.lowercase(Locale.getDefault()).removePrefix("gravityfactor:").toDoubleOrNull() ?: 1.8
                val model: Int = args.getOrNull(3)?.let { it.toIntOrNull() ?: VehicleModels.model.getOrDefault(it, VehicleModel("", 0)).customModelData } ?: 0

                val itemMaterial: Material = Material.entries.find { it.name == args.getOrNull(4) } ?: VehicleModels.model[args.getOrNull(3)]?.material ?: Material.DIAMOND

                VehicleEntity.spawn(location, isSmall, gravityFactor, model, itemMaterial)?.apply {
                    this.displayItem = this.displayItem.clone().apply {
                        this.editMeta {
                            it.persistentDataContainer.set(VehicleEntity.namespacedKey("speed_controls"), PersistentDataType.STRING, controlSpeeds)
                            it.persistentDataContainer.set(VehicleEntity.namespacedKey("owner"), PersistentDataType.STRING, player.uniqueId.toString())
                        }
                    }

                    this.mountDriver(player)
                }

                return@setExecutor true
            }

            cmd.setTabCompleter { sender, command, label, args ->
                return@setTabCompleter mutableListOf<String>().apply {
                    when (args.size) {
                        1 -> this.addAll(mutableListOf("small", "large", "help"))
                        2 -> this.addAll(mutableListOf("0.01,0.4,2.0", "0.03,0.5"))
                        3 -> this.addAll(mutableListOf("gravityFactor:", "gravityFactor:1", "gravityFactor:1.3", "gravityFactor:1.7", "gravityFactor:2", "gravityFactor:3"))
                        4 -> this.addAll(VehicleModels.model.keys)
                        5 -> this.addAll(VehicleModels.model[args.getOrNull(3)]?.let { mutableListOf(it.material.name) } ?: Material.entries.map { it.name })
                    }
                }
            }
        }
    }
}