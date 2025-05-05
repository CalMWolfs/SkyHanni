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
    private val maxDimension = maxOf(width, height)

    private val extraOffset = if (shape.isLongRoom) {
        maxDimension - 30
    } else {
        0
    }

    /**
     * For when converting a position relative to the room into a saved position (saving room)
     */
    fun relativeToActual(pos: LorenzVec): LorenzVec {
        val (unOffsetX, unOffsetZ) = pos.x - x to pos.z - z
        return when (rotation) {
            RoomRotation.NORTH -> LorenzVec(unOffsetX, pos.y, unOffsetZ)
            RoomRotation.EAST -> LorenzVec(unOffsetZ, pos.y, -unOffsetX + maxDimension)
            RoomRotation.SOUTH -> LorenzVec(-unOffsetX + maxDimension, pos.y, -unOffsetZ + maxDimension - extraOffset)
            RoomRotation.WEST -> LorenzVec(-unOffsetZ + maxDimension - extraOffset, pos.y, unOffsetX)
        }
    }

    /**
     * For when converting the saved position into a position relative to the room (loading room)
     */
    fun actualToRelative(pos: LorenzVec): LorenzVec {
        val (offsetX, offsetZ) = when (rotation) {
            RoomRotation.NORTH -> pos.x to pos.z
            RoomRotation.EAST -> -pos.z + maxDimension to pos.x
            RoomRotation.SOUTH -> -pos.x + maxDimension - extraOffset to -pos.z + maxDimension
            RoomRotation.WEST -> pos.z to -pos.x + maxDimension - extraOffset
        }
        return LorenzVec(offsetX + x, pos.y, offsetZ + z)
    }
}
