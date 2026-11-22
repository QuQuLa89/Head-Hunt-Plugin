package com.ququla89.headhunt.model

import org.bukkit.Location
import java.util.UUID

data class Treasure(
    val id: UUID,
    val worldName: String,
    val x: Int,
    val y: Int,
    val z: Int,
) {
    companion object {
        fun fromLocation(
            id: UUID,
            location: Location,
        ): Treasure =
            Treasure(
                id = id,
                worldName = requireNotNull(location.world) { "座標にはワールドが必要です。" }.name,
                x = location.blockX,
                y = location.blockY,
                z = location.blockZ,
            )
    }
}
