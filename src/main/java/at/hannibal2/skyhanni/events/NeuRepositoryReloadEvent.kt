package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.enoughupdates.EnoughUpdatesManager
import at.hannibal2.skyhanni.utils.json.fromJson
import at.hannibal2.skyhanni.utils.json.getJson
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import java.io.File

class NeuRepositoryReloadEvent : LorenzEvent() {
    fun getConstant(file: String): JsonObject? {
        return File(EnoughUpdatesManager.repoLocation, "constants/$file.json").getJson()
    }

    inline fun <reified T : Any> readConstant(file: String, gson: Gson = ConfigManager.gson): T {
        val data = getConstant(file) ?: ErrorManager.skyHanniError("$file failed to load from neu repo!")
        return try {
            gson.fromJson<T>(data)
        } catch (e: JsonSyntaxException) {
            ErrorManager.logErrorWithData(
                e, "$file failed to read from neu repo!",
                "data" to data,
            )
            throw e
        }
    }
}
