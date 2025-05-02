package at.hannibal2.skyhanni.test.graph

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.dungeon.DungeonData
import at.hannibal2.skyhanni.data.dungeon.DungeonRoomData
import at.hannibal2.skyhanni.data.model.Graph
import at.hannibal2.skyhanni.data.repo.RepoManager
import at.hannibal2.skyhanni.data.repo.RepoUtils
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import java.io.File

@SkyHanniModule
object DungeonGraphs {
    private val config get() = SkyHanniMod.feature.dev

    private var lastRoomId: String? = null
    private var previousRoom: DungeonRoomData? = null

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
                add("No data")
            } else {
                add("Room id: ${data.roomId}")
                add("Room corner: ${data.x}, ${data.z}")
                add("Room size: ${data.width}, ${data.height}")
                add("Room rotation: ${data.rotation}")
                add("Room shape: ${data.shape}")
                add("Room type: ${data.type}")
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    fun onTick() {
        if (!isEnabled()) return

        val newRoom = DungeonData.currentDungeonRoom
        val newRoomId = newRoom?.roomId

        if (newRoomId == lastRoomId) return

        lastRoomId?.let {
            saveRoom(it)
        }

        previousRoom = newRoom
        lastRoomId = newRoomId

        if (newRoomId == null) {
            GraphEditor.clear()
        } else {
            loadRoom(newRoomId)
        }
    }

    private fun loadRoom(roomId: String) {
        val file = File("config/skyhanni/repo/constants/island_graphs/dungeon_rooms/$roomId.json")
        if (!file.isFile) {
            GraphEditor.clear()
            ChatUtils.chat("No graph found for room $roomId")
            return
        }

        val graph = RepoUtils.getConstant(RepoManager.repoLocation, "island_graphs/dungeon_rooms/$roomId", Graph.gson, Graph::class.java)
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
        val file = File("config/skyhanni/repo/constants/island_graphs/dungeon_rooms/$roomId.json")

        file.parentFile?.mkdirs()
        file.writeText(json)
        ChatUtils.chat("Saved Room: $roomId")
    }

    private fun isEnabled() = DungeonApi.inDungeon() && !DungeonApi.inBossRoom && config.dungeonRoomDetection
}
