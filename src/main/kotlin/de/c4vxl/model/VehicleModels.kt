package de.c4vxl.model

import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

data class VehicleModel(val name: String, val customModelData: Int, val material: Material = Material.DIAMOND)

object VehicleModels {
    private val configFile: File = File("vehicleModels.yml").apply { if (!this.exists()) this.createNewFile() }
    val config: YamlConfiguration = YamlConfiguration.loadConfiguration(configFile)
    fun saveConfigChanges(): Boolean = config.save(configFile) == Unit
    val model: MutableMap<String, VehicleModel> get() = mutableMapOf<String, VehicleModel>().apply { config.getKeys(false).forEach {
        this[it] = VehicleModel(it, config.getInt("$it.model"), Material.entries.find { it.name == config.getString("$it.material") } ?: Material.DIAMOND)
    } }

    fun hasModel(name: String): Boolean = config.get(name) != null

    fun addModel(name: String, customModelData: Int = 0, material: Material = Material.DIAMOND): Boolean {
        if (hasModel(name)) return false

        config.set("$name.model", customModelData)
        config.set("$name.material", material.name)

        return saveConfigChanges()
    }

    fun removeModel(name: String): Boolean {
        config.set(name, null)
        return saveConfigChanges()
    }
}