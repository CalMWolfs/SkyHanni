package at.hannibal2.skyhanni.data.dungeon

import at.hannibal2.skyhanni.utils.LorenzVec

data class DungeonRoomData(
    val x: Int,
    val z: Int,
    val width: Int,
    val height: Int,
    val rotation: RoomRotation,
    val shape: RoomShape,
    val type: RoomType,
    val roomId: String,
) {
    fun relativeToActual(pos: LorenzVec): LorenzVec {
        return when (rotation) {
            RoomRotation.NORTH -> LorenzVec(pos.x + x, pos.y, pos.z + z)
            RoomRotation.EAST -> LorenzVec(-(pos.z - x), pos.y, pos.x + z)
            RoomRotation.SOUTH -> LorenzVec(-(pos.x - x), pos.y, -(pos.z - z))
            RoomRotation.WEST -> LorenzVec(pos.z + x, pos.y, -(pos.x - z))
        }
    }

    fun actualToRelative(pos: LorenzVec): LorenzVec {
        return when (rotation) {
            RoomRotation.NORTH -> LorenzVec(pos.x - x, pos.y, pos.z - z)
            RoomRotation.EAST -> LorenzVec(pos.z - z, pos.y, -(pos.x - x))
            RoomRotation.SOUTH -> LorenzVec(-(pos.x - x), pos.y, -(pos.z - z))
            RoomRotation.WEST -> LorenzVec(-(pos.z - z), pos.y, pos.x - x)
        }
    }
}

enum class RoomRotation {
    NORTH,
    EAST,
    SOUTH,
    WEST,
}
