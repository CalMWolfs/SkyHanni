package at.hannibal2.skyhanni.test.graph

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.dungeon.DungeonData
import at.hannibal2.skyhanni.data.dungeon.DungeonRoomData
import at.hannibal2.skyhanni.data.jsonobjects.repo.DungeonRoomInfo
import at.hannibal2.skyhanni.data.jsonobjects.repo.DungeonRoomOffset
import at.hannibal2.skyhanni.data.jsonobjects.repo.DungeonRoomsJson
import at.hannibal2.skyhanni.data.model.Graph
import at.hannibal2.skyhanni.data.repo.RepoManager
import at.hannibal2.skyhanni.data.repo.RepoUtils
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import java.awt.Point
import java.io.File

@SkyHanniModule
object DungeonGraphsEditor {
    private val config get() = SkyHanniMod.feature.dev

    private val drmLoaded by lazy { PlatformUtils.isModInstalled("dungeonrooms") }

    private var lastRoomId: String? = null
    private var previousRoom: DungeonRoomData? = null

    private var drmData: DrmRoomData? = null

    private const val CONFIG_LOCATION = "config/skyhanni/repo/constants"
    private const val GRAPH_LOCATION = "island_graphs/dungeon_rooms"

    private val roomInfo = mutableMapOf<String, DungeonRoomInfo>()
    private val roomOffsets = mutableMapOf<String, DungeonRoomOffset>()

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        roomInfo.clear()
        roomOffsets.clear()
        event.getConstant<DungeonRoomsJson>("DungeonRooms").rooms.forEach { (id, room) ->
            roomInfo[id] = room
        }
        event.getConstant<DungeonRoomsJson>("DungeonRooms").roomOffsets.forEach { (key, offset) ->
            roomOffsets[key] = offset
        }
    }

    /**
     * This is used for when you need to transform all the nodes in a rooms graph. Either rotated or offset.
     */
//     init {
//         val needsFlipping = listOf(
// //             "30,-384",
//             ""
//         )
//
//         val fakeRoomData = DungeonRoomData(
//             0,
//             0,
//             62,
//             62,
//             RoomRotation.SOUTH,
//             RoomShape.ONE_TWO,
//             at.hannibal2.skyhanni.data.dungeon.RoomType.NORMAL,
//             "fake",
//         )
//
//         for (need in needsFlipping) {
//             val file = File("$CONFIG_LOCATION/$GRAPH_LOCATION/$need.json")
//             if (!file.isFile) {
//                 continue
//             }
//
//             val graph = RepoUtils.getConstant(RepoManager.repoLocation, "$GRAPH_LOCATION/$need", Graph.gson, Graph::class.java)
//             if (graph.nodes.isEmpty()) {
//                 continue
//             }
// //             val transformedGraph = graph.transformNodes { fakeRoomData.actualToRelative(it) }
//             val transformedGraph = graph.transformNodes { it - LorenzVec(136, 0, 200) }
//             file.writeText(transformedGraph.toJson())
//         }
//     }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Dungeon Room Graph")

        if (!isEnabled()) {
            if (DungeonApi.inDungeon()) {
                event.addIrrelevant("Not in a dungeon")
            } else if (DungeonApi.inBossRoom) {
                event.addIrrelevant("In a boss room")
            } else {
                event.addIrrelevant("Disabled in config")
            }
            return
        }

        val data = DungeonData.currentDungeonRoom
        event.addData {
            if (data == null) {
                add("No Skyhanni room data")
            } else {
                add("SH room id: ${data.roomId}")
                add("SH room corner: ${data.x}, ${data.z}")
                add("SH room size: ${data.width}, ${data.height}")
                add("SH room rotation: ${data.rotation}")
                add("SH room shape: ${data.shape}")
                add("SH room type: ${data.type}")
            }
            if (drmLoaded) {
                drmData?.let { drm ->
                    add("DRM room name: ${drm.roomName}")
                    add("DRM room direction: ${drm.roomDirection}")
                    add("DRM room corner: ${drm.roomCorner}")
                } ?: add("No DRM room data")
            } else {
                add("DRM not loaded")
            }
        }
    }

    // TODO make using sh & drm together work much better
    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    fun onTick() {
        if (!isEnabled()) return

        val newRoom = DungeonData.currentDungeonRoom
        val newRoomId = newRoom?.roomId

        if (drmLoaded && drmData == null) {
            drmData = getDrmData()
        }

        if (newRoomId == lastRoomId) return

        lastRoomId?.let {
            saveRoom(it)
        }

        drmData = null

        previousRoom = newRoom
        lastRoomId = newRoomId

        if (newRoomId == null) {
            GraphEditor.clear()
        } else {
            loadRoom(newRoomId)
        }
    }

    @HandleEvent
    fun onWorldChange() {
        reset()
    }

    private fun getDrmData(): DrmRoomData? {
        if (!drmLoaded) return null

        val (name, direction, corner) = readRoomData() ?: return null
        return DrmRoomData(name, direction, corner)
    }

    // TODO use mixin
    private fun readRoomData(): Triple<String?, DrmRotation?, LorenzVec?>? =
        Class.forName("io.github.quantizr.dungeonrooms.dungeons.catacombs.RoomDetection")?.let { detection ->
            val name = detection.getField("roomName").get(null) as String?
            val direction = detection.getField("roomDirection").get(null) as String?
            val corner = (detection.getField("roomCorner").get(null) as Point?)?.let { LorenzVec(it.x, 0, it.y) }

            if (name == "undefined") return null

            Triple(name, DrmRotation.fromDrmName(direction), corner)
        }

    private enum class DrmRotation(val drmName: String) {
        NORTH("NW"),
        EAST("NE"),
        SOUTH("SE"),
        WEST("SW"),
        ;

        companion object {
            fun fromDrmName(name: String?): DrmRotation? {
                return entries.find { it.drmName == name }
            }
        }
    }

    private data class DrmRoomData(
        val roomName: String?,
        val roomDirection: DrmRotation?,
        val roomCorner: LorenzVec?,
    )

    private fun reset() {
        lastRoomId = null
        previousRoom = null
        drmData = null
    }

    private fun loadRoom(roomId: String) {
        val file = File("$CONFIG_LOCATION/$GRAPH_LOCATION/$roomId.json")
        if (!file.isFile) {
            GraphEditor.clear()
            ChatUtils.chat("No graph found for room $roomId")
            return
        }

        val graph = RepoUtils.getConstant(RepoManager.repoLocation, "$GRAPH_LOCATION/$roomId", Graph.gson, Graph::class.java)
        if (graph.nodes.isEmpty()) {
            GraphEditor.clear()
        } else {
            IslandGraphs.loadLobby("dungeon_rooms/$roomId")
            if (!GraphEditor.isEnabled()) return
            ChatUtils.chat("loaded room graph $roomId")

            val room = previousRoom ?: return

            GraphEditor.import(graph.transformNodes { room.actualToRelative(it) })
        }
    }

    private fun saveRoom(roomId: String) {
        if (!GraphEditor.isEnabled()) return
        val compiledGraph = GraphEditor.compileGraph()
        if (compiledGraph.nodes.isEmpty()) return
        val room = previousRoom ?: return

        val json = compiledGraph.transformNodes { room.relativeToActual(it) }.toJson()
        val file = File("$CONFIG_LOCATION/$GRAPH_LOCATION/$roomId.json")

        file.parentFile?.mkdirs()
        file.writeText(json)
        ChatUtils.chat("Saved Room: $roomId")

        val drmData = drmData ?: return
        val roomName = drmData.roomName ?: return

        val rotationOffset = drmData.roomDirection?.let { drmRotation ->
            if (room.shape.isLongRoom) {
                (room.rotation.ordinal - drmRotation.ordinal + 5) % 4
            } else {
                (room.rotation.ordinal - drmRotation.ordinal + 4) % 4
            }
        } ?: 0

        if (rotationOffset != 0) {
            ChatUtils.chat("Room rotation offset is not 0: $rotationOffset")
        }

        var needsSaving = false

        if (roomId !in roomInfo) {
            roomInfo[roomId] = DungeonRoomInfo(roomName, room.shape)
            ChatUtils.chat("Saved room data for id $roomId: $roomName")
            needsSaving = true
        }

        val roomCorner = drmData.roomCorner

        if (roomCorner != null && rotationOffset == 0) {
            val offsetKey = "${room.shape},${room.rotation}"

            val (offX, _, offZ) = roomCorner - LorenzVec(room.x, 0, room.z)
            val offsetX = offX.toInt()
            val offsetZ = offZ.toInt()

            val previousData = roomOffsets[offsetKey]

            if (previousData == null) {
                roomOffsets[offsetKey] = DungeonRoomOffset(offsetX, offsetZ)
                ChatUtils.chat("Saved offset for ${room.shape} rotated ${room.rotation}: $offsetX, $offsetZ")
                needsSaving = true
            } else if (previousData != DungeonRoomOffset(offsetX, offsetZ)) {
                ChatUtils.chat(
                    "Offset for ${room.shape} rotated ${room.rotation} is not the " +
                        "same as previous: $offsetX, $offsetZ != ${previousData.x}, ${previousData.z}. " +
                        "Name: $roomName and id: $roomId",
                )
            }
        }

        if (!needsSaving) return
        val constantsFile = File("$CONFIG_LOCATION/DungeonRooms.json")
        constantsFile.writeText(ConfigManager.gson.toJson(DungeonRoomsJson(roomInfo, roomOffsets)))
    }

//     private fun roomIdFromName(name: String): String? {
//         val matches = roomInfo.entries.filter { it.value.name == name }
//         if (matches.isEmpty() || matches.size > 1) return null
//         return matches.first().key
//     }

    private fun isEnabled() = DungeonApi.inDungeon() && !DungeonApi.inBossRoom && config.dungeonRoomDetection
}
