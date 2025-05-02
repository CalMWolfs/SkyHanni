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

    /**
     * For when converting a position relative to the room into a saved position (saving room)
     */
    fun relativeToActual(pos: LorenzVec): LorenzVec {
        val (unOffsetX, unOffsetZ) = pos.x - x to pos.z - z
        return when (rotation) {
            RoomRotation.NORTH -> LorenzVec(unOffsetX, pos.y, unOffsetZ)
            RoomRotation.EAST -> LorenzVec(unOffsetZ, pos.y, -unOffsetX + width + DungeonData.DOOR_SIZE)
            RoomRotation.SOUTH -> LorenzVec(-unOffsetX + width + DungeonData.DOOR_SIZE, pos.y, -unOffsetZ + height + DungeonData.DOOR_SIZE)
            RoomRotation.WEST -> LorenzVec(-unOffsetZ + height + DungeonData.DOOR_SIZE, pos.y, unOffsetX)
        }
    }

    /**
     * For when converting the saved position into a position relative to the room (loading room)
     */
    fun actualToRelative(pos: LorenzVec): LorenzVec {
        val (offsetX, offsetZ) = when (rotation) {
            RoomRotation.NORTH -> pos.x to pos.z
            RoomRotation.EAST -> -pos.z + height + DungeonData.DOOR_SIZE to pos.x
            RoomRotation.SOUTH -> -pos.x + width + DungeonData.DOOR_SIZE to -pos.z + height + DungeonData.DOOR_SIZE
            RoomRotation.WEST -> pos.z to -pos.x + width + DungeonData.DOOR_SIZE
        }
        return LorenzVec(offsetX + x, pos.y, offsetZ + z)
    }
}
