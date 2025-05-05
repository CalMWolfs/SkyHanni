package at.hannibal2.skyhanni.data.dungeon

data class MapColorArray(private val colors: Array<IntArray>) {
    override fun toString(): String {
        val result = StringBuilder()
        for (row in colors) {
            result.append(row.joinToString(",")).append("\n")
        }
        return result.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MapColorArray) return false

        return colors.contentDeepEquals(other.colors)
    }

    operator fun get(x: Int, y: Int): Int {
        return colors[x][y]
    }

    operator fun get(pair: DungeonPos): Int {
        return colors[pair.x][pair.y]
    }

    fun getColors(): List<List<Int>> {
        return colors.map { it.toList() }
    }

    override fun hashCode(): Int {
        return colors.contentDeepHashCode()
    }

    companion object {
        fun empty() = MapColorArray(Array(128) { IntArray(128) })
    }
}
