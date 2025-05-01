package at.hannibal2.skyhanni.data.dungeon

import at.hannibal2.skyhanni.utils.ChatUtils

data class DungeonRoom(val components: MutableList<DungeonPos>, var type: RoomType) {

    private val xComps: Set<Int>
        get() = components.map { it.x }.toSet()

    private val yComps: Set<Int>
        get() = components.map { it.y }.toSet()

    val width: Int
        get() = xComps.size * DungeonData.ROOM_SIZE - DungeonData.DOOR_SIZE

    val height: Int
        get() = yComps.size * DungeonData.ROOM_SIZE - DungeonData.DOOR_SIZE

    val topLeftPos = DungeonPos(
        components.minOf { it.x },
        components.minOf { it.y }
    )

    val shape: RoomShape
        get() {
            val size = components.size

            if (size == 1) return RoomShape.ONE_ONE
            if (size == 2) return RoomShape.ONE_TWO

            if (xComps.size == 2 && yComps.size == 2 && size == 4) return RoomShape.TWO_TWO
            if (xComps.size == 1 || yComps.size == 1) {
                if (size == 3) return RoomShape.ONE_THREE
                if (size == 4) return RoomShape.ONE_FOUR
            }

            println("Is L-Shape")
            return RoomShape.L_SHAPE
        }

    /**
     * Returns null for 1x1 rooms
     */
    val rotation: RoomRotation?
        get() {
            if (type == RoomType.FAIRY) return RoomRotation.EAST
            val shape = this.shape
            if (shape == RoomShape.ONE_ONE) return null
            if (shape == RoomShape.TWO_TWO) return RoomRotation.EAST

            if (shape in listOf(RoomShape.ONE_TWO, RoomShape.ONE_THREE, RoomShape.ONE_FOUR)) {
                if (xComps.size == 1) return RoomRotation.EAST
                if (yComps.size == 1) return RoomRotation.NORTH
            }

            // L-Shape
            val intersection = components.find { component ->
                components.count { it.x == component.x } > 1 && components.count { it.y == component.y } > 1
            }

            val intersectionX = intersection?.x ?: return null
            val intersectionY = intersection.y

            val minX = components.minOf { it.x }
            val minY = components.minOf { it.y }
            val maxX = components.maxOf { it.x }
            val maxY = components.maxOf { it.y }

            if (intersectionX == minX && intersectionY == maxY) return RoomRotation.NORTH
            if (intersectionX == maxX && intersectionY == maxY) return RoomRotation.EAST
            if (intersectionX == maxX && intersectionY == minY) return RoomRotation.SOUTH
            if (intersectionX == minX && intersectionY == minY) return RoomRotation.WEST
            ChatUtils.chat("Unknown room rotation: $this")
            println("Unknown room rotation: $this")
            println("Intersection: $intersection")
            println("minX: $minX, minY: $minY, maxX: $maxX, maxY: $maxY")

            return null
        }
}

enum class RoomShape {
    ONE_ONE,
    ONE_TWO,
    ONE_THREE,
    ONE_FOUR,
    TWO_TWO,
    L_SHAPE,
}

enum class RoomType(val mapColor: Int) {
    SPAWN(30),
    NORMAL(63),
    PUZZLE(66),
    MINIBOSS(74),
    FAIRY(82),
    TRAP(62),
    BLOOD(62),
    UNOPENED(85),
    UNKNOWN(0);

    companion object {
        fun fromMapColor(mapColor: Int): RoomType {
            return entries.firstOrNull { it.mapColor == mapColor } ?: UNKNOWN
        }
    }
}

