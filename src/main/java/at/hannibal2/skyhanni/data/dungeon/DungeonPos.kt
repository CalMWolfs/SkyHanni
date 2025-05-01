package at.hannibal2.skyhanni.data.dungeon

data class DungeonPos(val x: Int = -1, val y: Int = -1) {
    override fun toString(): String {
        return "(x: $x, y: $y)"
    }
}
