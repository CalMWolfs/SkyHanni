package at.hannibal2.skyhanni.data.dungeon

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.ScoreboardUpdateEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockIdAt
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockMetadataAt
import at.hannibal2.skyhanni.utils.BlockUtils.isAir
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.minecraft.item.ItemMap
import kotlin.math.floor

// TODO add isEnabled check
@SkyHanniModule
object DungeonData {

    const val ROOM_SIZE = 32
    const val DOOR_SIZE = 2

    private const val ROOM_OFFSET = 200

    private var mapCache = MapColorArray.empty()
    private val dungeonRooms = mutableMapOf<DungeonPos, DungeonRoom>()

    // TODO use cache when going back to a known room
    private val roomData = mutableMapOf<DungeonPos, DungeonRoomData>()

    private val doors = mutableListOf<DungeonDoor>()
    private val witherDoors = mutableListOf<DungeonDoor>()

    private var topLeftTilePos = DungeonPos()
    private var mapTileCount = DungeonPos()

    private var tileSize: Int? = null
    private var roomId: String? = null
    private var currentDungeonRoom: DungeonRoomData? = null

    private var scaleFactor = 0.0

    private val mapTileSize: Int
        get() = (tileSize ?: 0) + DOOR_SIZE * 2

    private val currentRoom
        get() =
            dungeonRooms[gridPosFromMapPos(mapPosFromWorldPos(LocationUtils.playerLocation().toRoomTopCorner()))]

    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    fun onScoreboardUpdate(event: ScoreboardUpdateEvent) {
        DungeonApi.dungeonRoomPattern.firstMatcher(event.added) {
            val detectedId = group("roomId")
            if (roomId != detectedId) {
                roomId = detectedId
                currentDungeonRoom = null
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    fun onTick() {
        val mapStack = InventoryUtils.getItemsInOwnInventoryWithNull()?.get(8) ?: return
        val mapItem = mapStack.item
        if (mapItem !is ItemMap) return

        val mapData = mapItem.getMapData(mapStack, MinecraftCompat.localWorld) ?: return

        val pixelData: Array<Array<Int>> = Array(128) { Array(128) { 0 } }
        val mapColors = mapData.colors

        for (x in 0 until 128) {
            for (y in 0 until 128) {
                val color = mapColors[x + y * 128].toInt()
                pixelData[x][y] = color
            }
        }
        val mapColorArray = MapColorArray(pixelData)

        mapCache = mapColorArray
        // If we already have room data we don't need to update the map
        if (currentDungeonRoom != null || roomId == null) return
        updateMap()
    }

    private fun setCurrentRoomData(room: DungeonRoomData, cornerPos: DungeonPos) {
        roomData[cornerPos] = room
        currentDungeonRoom = room
    }

    private fun updateMap() {
        if (tileSize == null) {
            findMapSize()
        }
        tileSize ?: return

        scanMap()
    }

    private fun findMapSize() {
        var spawnMapPos = DungeonPos()
        var tileCount = 0

        for (x in mapCache.getColors().indices) {
            for (y in mapCache.getColors()[x].indices) {
                when {
                    spawnMapPos != DungeonPos() && mapCache[x, y] != RoomType.SPAWN.mapColor -> {
                        setUpMap(spawnMapPos, tileCount)
                        return
                    }

                    spawnMapPos != DungeonPos() -> {
                        tileCount++
                    }

                    mapCache[x, y] == RoomType.SPAWN.mapColor -> {
                        spawnMapPos = DungeonPos(x, y)
                        tileCount++
                    }
                }
            }
        }
    }

    private fun setUpMap(spawnMapPos: DungeonPos, tileCount: Int) {
        if (tileCount != 16 && tileCount != 18) {
            return
        }
        tileSize = tileCount

        scaleFactor = mapTileSize / ROOM_SIZE.toDouble()

        val (startX, countX) = getPosAndSize(spawnMapPos.x)
        val (startY, countY) = getPosAndSize(spawnMapPos.y)

        topLeftTilePos = DungeonPos(startX, startY)
        mapTileCount = DungeonPos(countX, countY)
    }

    private fun getPosAndSize(coordinate: Int): Pair<Int, Int> {
        return if (tileSize == 16) {
            if (coordinate % 2 == 0) 14 to 5 else 3 to 6
        } else {
            if (coordinate % 2 == 0) 20 to 4 else 9 to 5
        }
    }

    private fun scanMap() {
        for (y in 0 until mapTileCount.y) {
            for (x in 0 until mapTileCount.x) {
                val location = mapPosFromGridPos(DungeonPos(x, y), DungeonPos(DOOR_SIZE, DOOR_SIZE))

                val color = mapCache[location]
                if (color == RoomType.UNKNOWN.mapColor || color == RoomType.UNOPENED.mapColor) {
                    continue
                }
                processRoom(DungeonPos(x, y), color)
                processRoomDoors(DungeonPos(x, y))
            }
        }

        val currentRoom = currentRoom ?: return
        currentRoom.rotation?.let { rotation ->
            val cornerPos = worldPosFromMapPos(mapPosFromGridPos(currentRoom.topLeftPos))
            val newRoom = DungeonRoomData(
                cornerPos.x,
                cornerPos.y,
                currentRoom.width,
                currentRoom.height,
                rotation,
                currentRoom.shape,
                currentRoom.type,
                roomId ?: "Unknown",
            )
            setCurrentRoomData(newRoom, currentRoom.topLeftPos)
            return
        }

        findRotationNew(currentRoom)
    }

    private fun processRoom(pos: DungeonPos, color: Int) {
        val currentRoom = dungeonRooms[pos]

        val leftRoom = if (mapCache[mapPosFromGridPos(pos, DungeonPos(0, DOOR_SIZE))] == RoomType.NORMAL.mapColor) {
            dungeonRooms[DungeonPos(pos.x - 1, pos.y)]
        } else null

        val topRoom = if (mapCache[mapPosFromGridPos(pos, DungeonPos(DOOR_SIZE, 0))] == RoomType.NORMAL.mapColor) {
            dungeonRooms[DungeonPos(pos.x, pos.y - 1)]
        } else null

        // Checking if the room to the right connects left and up for the 1 rotation of L shaped rooms
        val topRightRoom =
            if (mapCache[mapPosFromGridPos(DungeonPos(pos.x + 1, pos.y), DungeonPos(DOOR_SIZE, 0))] == RoomType.NORMAL.mapColor &&
                mapCache[mapPosFromGridPos(DungeonPos(pos.x + 1, pos.y), DungeonPos(0, DOOR_SIZE))] == RoomType.NORMAL.mapColor
            ) {
                dungeonRooms[DungeonPos(pos.x + 1, pos.y - 1)]
            } else null

        // add new room
        if (currentRoom == null && leftRoom == null && topRoom == null) {
            dungeonRooms[pos] = DungeonRoom(mutableListOf(pos), RoomType.fromMapColor(color))
        }

        // merge left
        if (leftRoom != null && currentRoom != leftRoom && !leftRoom.components.contains(pos)) {
            leftRoom.components.add(pos)
            dungeonRooms[pos] = leftRoom
        }

        // merge top
        if (topRoom != null && currentRoom != topRoom && !topRoom.components.contains(pos)) {
            topRoom.components.add(pos)
            dungeonRooms[pos] = topRoom
        }

        // merge top right
        if (topRightRoom != null && currentRoom != topRightRoom && !topRightRoom.components.contains(pos)) {
            topRightRoom.components.add(pos)
            dungeonRooms[pos] = topRightRoom
        }
    }

    private fun findRotationNew(room: DungeonRoom) {
        if (room.shape != RoomShape.ONE_ONE) {
            ErrorManager.skyHanniError("Rotation wasn't found and room shape was not 1x1")
        }

        val cornerPos = worldPosFromMapPos(mapPosFromGridPos(room.topLeftPos))
        val roofY = getRoofHeight(cornerPos.x, cornerPos.y)

        val p1 = LorenzVec(cornerPos.x, roofY, cornerPos.y).isCorrectBlock()
        val p2 = LorenzVec(cornerPos.x + ROOM_SIZE - DOOR_SIZE, roofY, cornerPos.y).isCorrectBlock()
        val p3 = LorenzVec(cornerPos.x, roofY, cornerPos.y + ROOM_SIZE - DOOR_SIZE).isCorrectBlock()
        val p4 = LorenzVec(cornerPos.x + ROOM_SIZE - DOOR_SIZE, roofY, cornerPos.y + ROOM_SIZE - DOOR_SIZE).isCorrectBlock()

        val rotation: RoomRotation? = if (p1 && !p2 && !p3 && !p4) {
            RoomRotation.NORTH
        } else if (!p1 && p2 && !p3 && !p4) {
            RoomRotation.EAST
        } else if (!p1 && !p2 && p3 && !p4) {
            RoomRotation.WEST
        } else if (!p1 && !p2 && !p3 && p4) {
            RoomRotation.SOUTH
        } else {
            null
        }

        rotation?.let {
            val newRoom = DungeonRoomData(
                cornerPos.x,
                cornerPos.y,
                room.width,
                room.height,
                it,
                room.shape,
                room.type,
                roomId ?: "Unknown",
            )
            setCurrentRoomData(newRoom, room.topLeftPos)
            return
        }

        ErrorManager.skyHanniError(
            "Room rotation wasn't able to be found",
            "p1, p2, p3, p4" to "$p1, $p2, $p3, $p4",
            "roomID" to roomId,
        )
    }

    private fun processRoomDoors(pos: DungeonPos) {
        // left door
        if (mapCache[mapPosFromGridPos(pos, DungeonPos(0, DOOR_SIZE))] == RoomType.UNKNOWN.mapColor &&
            mapCache[mapPosFromGridPos(pos, DungeonPos(0, mapTileSize / 2))] != RoomType.UNKNOWN.mapColor
        ) {
            val doorType = RoomType.fromMapColor(mapCache[mapPosFromGridPos(pos, DungeonPos(0, mapTileSize / 2))])
            updateDoor(pos, doorType, false)
        }

        // top door
        if (mapCache[mapPosFromGridPos(pos, DungeonPos(DOOR_SIZE, 0))] == RoomType.UNKNOWN.mapColor &&
            mapCache[mapPosFromGridPos(pos, DungeonPos(mapTileSize / 2, 0))] != RoomType.UNKNOWN.mapColor
        ) {
            val doorType = RoomType.fromMapColor(mapCache[mapPosFromGridPos(pos, DungeonPos(mapTileSize / 2, 0))])
            updateDoor(pos, doorType, true)
        }
    }

    private fun updateDoor(pos: DungeonPos, doorType: RoomType, horizontal: Boolean) {
        val door = doors.find { it.pos == pos && it.horizontal == horizontal }
        if (door == null) {
            val newDoor = DungeonDoor(doorType, pos, horizontal)
            doors.add(newDoor)
            if (doorType == RoomType.WITHER || RoomType.BLOOD == doorType) {
                witherDoors.add(newDoor)
            }
        } else if (doorType != door.type) {
            if (door in witherDoors) {
                witherDoors.remove(door)
            }
            door.type = doorType
        }
    }

    private fun LorenzVec.isCorrectBlock(): Boolean {
        return getBlockIdAt() == 159 && getBlockMetadataAt() == 11
    }

    private fun getRoofHeight(x: Int, z: Int): Int {
        var y = 255
        while (y > 0 && LorenzVec(x, y, z).isAir()) y--

        return y
    }

    @HandleEvent
    fun onWorldChange() {
        reset()
    }

    private fun reset() {
        tileSize = null
        roomId = null
        currentDungeonRoom = null

        mapCache = MapColorArray.empty()
        topLeftTilePos = DungeonPos()
        mapTileCount = DungeonPos()

        dungeonRooms.clear()
        roomData.clear()
        doors.clear()
        witherDoors.clear()
    }

    private fun mapPosFromGridPos(gridPos: DungeonPos, offset: DungeonPos = DungeonPos(0, 0)): DungeonPos {
        val x = gridPos.x * mapTileSize + topLeftTilePos.x + offset.x
        val y = gridPos.y * mapTileSize + topLeftTilePos.y + offset.y
        return DungeonPos(x, y)
    }

    private fun gridPosFromMapPos(mapPos: DungeonPos): DungeonPos {
        val x = ((mapPos.x - topLeftTilePos.x) / mapTileSize)
        val y = ((mapPos.y - topLeftTilePos.y) / mapTileSize)
        return DungeonPos(x, y)
    }

    private fun worldPosFromMapPos(mapPos: DungeonPos): DungeonPos {
        val x = ((mapPos.x - topLeftTilePos.x) / scaleFactor).toInt() - ROOM_OFFSET
        val y = ((mapPos.y - topLeftTilePos.y) / scaleFactor).toInt() - ROOM_OFFSET
        return DungeonPos(x, y)
    }

    private fun mapPosFromWorldPos(worldPos: DungeonPos): DungeonPos {
        val x = ((worldPos.x + ROOM_OFFSET) * scaleFactor + topLeftTilePos.x).toInt()
        val y = ((worldPos.y + ROOM_OFFSET) * scaleFactor + topLeftTilePos.y).toInt()
        return DungeonPos(x, y)
    }

    private fun LorenzVec.toRoomTopCorner(): DungeonPos {
        val x = floor((x + 8) / 32).toInt() * 32 - 8
        val y = floor((z + 8) / 32).toInt() * 32 - 8
        return DungeonPos(x, y)
    }
}
