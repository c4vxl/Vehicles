package de.c4vxl.vehicle

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.*
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.EulerAngle
import org.bukkit.util.Vector

class VehicleEntity(val isSmall: Boolean = false, val gravitationFactor: Double = 2.8, val modelData: Int = 0, val displayItemMaterial: Material = Material.DIAMOND) {
    companion object {
        fun namespacedKey(key: String): NamespacedKey = NamespacedKey("c4vxl_vehicles", key)

        fun isVehicle(entity: Entity): Boolean =
            entity.takeIf { it is ArmorStand }?.persistentDataContainer?.get(namespacedKey("is_vehicle"), PersistentDataType.BOOLEAN) ?: false
        fun asVehicle(entity: Entity): VehicleEntity? =
            (entity as? ArmorStand)?.takeIf { it.isVehicleEntity }?.let { armorStand -> VehicleEntity(armorStand.isSmall, armorStand.getItem(EquipmentSlot.HEAD).takeIf { it.hasItemMeta() }?.persistentDataContainer?.get(namespacedKey("gravitation"), PersistentDataType.DOUBLE) ?: 2.4, armorStand.getItem(EquipmentSlot.HEAD).takeIf { it.hasItemMeta() }?.itemMeta?.customModelData ?: 0, armorStand.getItem(EquipmentSlot.HEAD).type).apply { displayItemHolder = armorStand } }

        fun isVehicleDisplayItem(item: ItemStack): Boolean =
            item.persistentDataContainer.get(namespacedKey("is_vehicle"), PersistentDataType.BOOLEAN) ?: false
        fun formItem(item: ItemStack): VehicleEntity? =
            if (!isVehicleDisplayItem(item)) null else VehicleEntity(
                item.persistentDataContainer.get(namespacedKey("small"), PersistentDataType.BOOLEAN) ?: false,
                item.persistentDataContainer.get(namespacedKey("gravitation"), PersistentDataType.DOUBLE) ?: 2.8,
                item.takeIf { it.hasItemMeta() }?.itemMeta?.takeIf { it.hasCustomModelData() }?.customModelData ?: 0,
                item.type
            )


        val Entity.isVehicleEntity: Boolean get() = isVehicle(this)
        val Entity.asVehicleEntity: VehicleEntity? get() = asVehicle(this)
        val Player.isOnVehicleEntity: Boolean get() = this.vehicleEntity != null
        val Player.vehicleEntity: VehicleEntity? get() = this.vehicle?.asVehicleEntity

        fun spawn(location: Location,
                  isSmall: Boolean = false,
                  gravitationFactor: Double = 2.8,
                  modelData: Int = 0,
                  displayItemMaterial: Material = Material.DIAMOND): VehicleEntity? = VehicleEntity(isSmall, gravitationFactor, modelData, displayItemMaterial).spawn(location)
    }

    lateinit var displayItemHolder: ArmorStand
    var displayItem: ItemStack
        get() = displayItemHolder.getItem(EquipmentSlot.HEAD).clone()
        set(value) = displayItemHolder.setItem(EquipmentSlot.HEAD, value)
    val exists: Boolean get() = this::displayItemHolder.isInitialized && !displayItemHolder.isDead
    val location: Location get() = displayItemHolder.location

    // Logic to get the driver of the vehicle
    val driver: Player? get() = displayItemHolder.passengers.find { it is Player } as? Player
    val hasDriver: Boolean get() = driver != null

    fun spawn(location: Location): VehicleEntity? {
        if (exists) return null

        // initialize display item holder
        this.displayItemHolder = (location.world.spawnEntity(location, EntityType.ARMOR_STAND) as ArmorStand).apply {
            this.isInvisible = true
            this.isSmall = isSmall
            this.isInvulnerable = true
            this.setPose(Pose.SLEEPING, true)

            this.persistentDataContainer.set(namespacedKey("is_vehicle"), PersistentDataType.BOOLEAN, true)
        }

        // initialize display item
        displayItem = ItemStack(displayItemMaterial).apply {
            this.editMeta {
                it.persistentDataContainer.set(namespacedKey("is_vehicle"), PersistentDataType.BOOLEAN, true)
                it.persistentDataContainer.set(namespacedKey("small"), PersistentDataType.BOOLEAN, this@VehicleEntity.isSmall)
                it.persistentDataContainer.set(namespacedKey("gravitation"), PersistentDataType.DOUBLE, gravitationFactor)

                it.setCustomModelData(modelData)
            }
        }

        return this
    }

    // remove car
    fun remove() {
        displayItemHolder.remove()
    }

    // rotating the head of the model
    fun rotateX(rotation: Double) {
        displayItemHolder.headPose = EulerAngle(rotation, 0.0, 0.0)
    }

    // mount and dismount driver
    fun mountDriver(player: Player): Boolean = displayItemHolder.takeIf { !hasDriver }?.addPassenger(player) ?: false
    fun dismountDriver(): Boolean = driver?.leaveVehicle() ?: false

    // set velocity to the car in a certain direction
    // movement logic of the car
    // checks for collision
    fun move(x: Double, y: Double, z: Double) = move(Vector(x, y, z)) // mapping to convert double into 3 dimensional vector
    fun move(vec3: Vector) {
        // Check if the car can move to the specified location
        fun canMoveTo(location: Location): Boolean = !location.block.isSolid


        val futureLocation: Location = location.clone().add(vec3)

        val canGoDown: Boolean = canMoveTo(futureLocation.clone().subtract(0.0, 1.0, 0.0))
        val canGoStraight: Boolean = canMoveTo(futureLocation)
        val canGoUp: Boolean = canMoveTo(futureLocation.clone().add(0.0, 1.0, 0.0))

        // Adjust the future location based on the movement nature
        if (canGoDown && canGoStraight) vec3.subtract(Vector(0.0, gravitationFactor, 0.0)) // apply gravity when car can go down
        else if (canGoStraight) { } // don't modify vector (car can go straight without any obstacles in the way)
        else if (canGoUp) vec3.add(Vector(0.0, 0.5, 0.0)) // push car up
        else {} // handle car getting stuck

        // rotate car when going up and down
        val heightDifference: Double = location.clone().add(vec3).y - location.y
        val threshold = 0.3
        rotateX(when {
            heightDifference > threshold -> -170.0
            heightDifference < -threshold -> 170.0
            else -> 0.0
        })

        // Set the car's velocity
        displayItemHolder.velocity = vec3
    }

    // drive logic
    fun driveTick(speed: Double) = move(displayItemHolder.location.direction.multiply(1 + speed).setY(0))
}