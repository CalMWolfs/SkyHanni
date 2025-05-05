package at.hannibal2.skyhanni.data.jsonobjects.repo

import at.hannibal2.skyhanni.data.dungeon.RoomShape
import com.google.gson.annotations.Expose

data class DungeonRoomsJson(
    @Expose val rooms: Map<String, DungeonRoomInfo>,
    @Expose val roomOffsets: Map<String, DungeonRoomOffset> = emptyMap(),
)

data class DungeonRoomInfo(
    @Expose val name: String,
    @Expose val roomShape: RoomShape,
)

data class DungeonRoomOffset(
    @Expose val x: Int,
    @Expose val z: Int,
)
