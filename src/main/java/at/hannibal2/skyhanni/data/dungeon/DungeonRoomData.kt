package at.hannibal2.skyhanni.data.dungeon

data class DungeonRoomData(
    val x: Int,
    val z: Int,
    val width: Int,
    val height: Int,
    val rotation: RoomRotation,
    val shape: RoomShape,
    val type: RoomType,
    val roomId: String,
)

enum class RoomRotation {
    NORTH,
    EAST,
    SOUTH,
    WEST,
}
