package at.hannibal2.skyhanni.api.enoughupdates.datatypes

import at.hannibal2.skyhanni.api.enoughupdates.EnoughUpdatesManager
import com.google.gson.JsonObject

data class Recipe(
    val ingredients: Set<Ingredient>,
    val outputs: Set<Ingredient>,
    val recipeType: RecipeType,
    val shouldUseForCraftCost: Boolean = true
) {

    companion object {
        fun loadRecipe(recipeJson: JsonObject, itemJson: JsonObject) {
            val type = recipeJson["type"]?.asString

            when (type) {
                "forge" -> {
                    val ingredients = mutableSetOf<Ingredient>()
                    for (ingredient in recipeJson["inputs"].asJsonArray) {
                        ingredients.add(Ingredient(ingredient.asString))
                    }

                    submitRecipe(ingredients, recipeJson, itemJson, RecipeType.FORGE)
                }

                "trade" -> {
                    val output = setOf(Ingredient(recipeJson["result"].asString))
                    if (recipeJson.has("max")) {
                        val minAmount = recipeJson["min"].asInt
                        val maxAmount = recipeJson["max"].asInt
                        val average = (minAmount + maxAmount) / 2

                        val recipe = Recipe(setOf(Ingredient(recipeJson["cost"].asString, average)), output, RecipeType.TRADE,false)
                        EnoughUpdatesManager.registerRecipe(recipe)
                    } else {
                        val recipe = Recipe(setOf(Ingredient(recipeJson["cost"].asString)), output, RecipeType.TRADE, false)
                        EnoughUpdatesManager.registerRecipe(recipe)
                    }
                }

                "drops" -> {
                    val ingredient = setOf(Ingredient(itemJson["internalname"].asString))
                    val outputs = mutableSetOf<Ingredient>()

                    for (output in recipeJson["drops"].asJsonArray) {
                        outputs.add(Ingredient(output.asJsonObject["id"].asString))
                    }
                    val recipe = Recipe(ingredient, outputs, RecipeType.MOB_DROP, false)
                    EnoughUpdatesManager.registerRecipe(recipe)
                }

                "npc_shop" -> {
                    val ingredients = mutableSetOf<Ingredient>()
                    for (ingredient in recipeJson["cost"].asJsonArray) {
                        ingredients.add(Ingredient(ingredient.asString))
                    }
                    val output = setOf(Ingredient(recipeJson["result"].asString))
                    val recipe = Recipe(ingredients, output, RecipeType.NPC_SHOP)
                    EnoughUpdatesManager.registerRecipe(recipe)
                }

                "katgrade" -> {
                    val ingredients = mutableSetOf<Ingredient>()
                    for (ingredient in recipeJson["items"].asJsonArray) {
                        ingredients.add(Ingredient(ingredient.asString))
                    }
                    ingredients.add(Ingredient(recipeJson["input"].asString))
                    ingredients.add(Ingredient.coinIngredient(recipeJson["coins"].asDouble))

                    val output = setOf(Ingredient(recipeJson["output"].asString))
                    val recipe = Recipe(ingredients, output, RecipeType.KAT_UPGRADE, false)
                    EnoughUpdatesManager.registerRecipe(recipe)
                }

                else -> {
                    val ingredients = mutableSetOf<Ingredient>()

                    val x = arrayOf("1", "2", "3")
                    val y = arrayOf("A", "B", "C")
                    for (i in 0..8) {
                        val name = y[i / 3] + x[i % 3]
                        recipeJson[name]?.asString?.let {
                            if (it.isNotEmpty()) ingredients.add(Ingredient(it))
                        }
                    }

                    submitRecipe(ingredients, recipeJson, itemJson, RecipeType.CRAFTING)
                }
            }
        }

        private fun submitRecipe(
            ingredients: Set<Ingredient>,
            recipeJson: JsonObject,
            itemJson: JsonObject,
            recipeType: RecipeType
        ) {
            val craftAmount = if (recipeJson.has("count")) recipeJson.get("count").asInt else 1
            val outputInternalName =
                if (recipeJson.has("overrideOutputId")) recipeJson.get("overrideOutputId").asString else itemJson.get("internalname").asString
            val outputItem = Ingredient(outputInternalName, craftAmount)

            val recipe = Recipe(ingredients, setOf(outputItem), recipeType)
            EnoughUpdatesManager.registerRecipe(recipe)
        }
    }
}

enum class RecipeType {
    FORGE,
    TRADE,
    MOB_DROP,
    NPC_SHOP,
    KAT_UPGRADE,
    CRAFTING,
}
