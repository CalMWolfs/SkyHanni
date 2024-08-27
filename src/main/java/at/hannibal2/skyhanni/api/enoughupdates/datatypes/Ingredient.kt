package at.hannibal2.skyhanni.api.enoughupdates.datatypes

import at.hannibal2.skyhanni.api.enoughupdates.EnoughUpdatesManager
import at.hannibal2.skyhanni.utils.ItemUtils
import net.minecraft.item.ItemStack

class Ingredient(val internalName: String, val count: Double = 1.0) {

    var itemStack: ItemStack? = null
        get() {
            if (field == null) {
                if (isCoin()) {
                    field = ItemUtils.createCoinItemStack(count)
                } else {
                    val itemInfo = EnoughUpdatesManager.getItemById(internalName)
                    val itemStack = EnoughUpdatesManager.jsonToStack(itemInfo)
                    itemStack.stackSize = count.toInt()
                    field = itemStack
                }
            }
            return field
        }

    constructor(internalName: String, count: Int) : this(internalName, count.toDouble())

    constructor(ingredientIdentifier: String) : this(
        ingredientIdentifier.substringBefore(':'),
        ingredientIdentifier.substringAfter(':').toDoubleOrNull() ?: 1.0,
    )

    companion object {
        const val SKYBLOCK_COIN = "SKYBLOCK_COIN"

        fun coinIngredient(count: Double = 1.0) = Ingredient(SKYBLOCK_COIN, count)
    }

    fun isCoin() = internalName == SKYBLOCK_COIN

    override fun toString() = "$internalName x$count"
}
