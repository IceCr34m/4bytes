package org.ethereum.lists.methodsignatures

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.lang.Thread.sleep
import java.util.concurrent.TimeUnit

const val PAGE_SIZE = 2000
const val url = "https://www.4byte.directory/api/v1/signatures/?page_size=$PAGE_SIZE&ordering=created_at"

val client = OkHttpClient.Builder().apply {
    readTimeout(42, TimeUnit.SECONDS)
}.build()


var total = 0

fun main() {
    import(url)
}

private fun import(url: String): Unit? = try {
    val request = Request.Builder().url(url).build()

    val response = client.newCall(request).execute()
    when (response.code) {
        200 -> response.body?.use { body ->
            val string = body.string()

            val jsonObject = Klaxon().parseJsonObject(string.reader())
            val array = jsonObject["results"] as JsonArray<*>

            var new = 0
            array.map { it as JsonObject }.forEach {
                val hexSignature = it["hex_signature"] as String
                val textSignature = it["text_signature"] as String
                if (store.upsert(hexSignature.replace("0x", ""), textSignature)) {
                    new++
                    total++
                }
            }
            println("processed: ${array.size} - imported: $new - total: $total")
            (jsonObject["next"] as String?)?.let {
                import(it)
            }
        }
        else -> error("Could not get $url")
    }
} catch (e: IOException) {
    println("Error fetching " + e.message)
    println("Retry in 4s")
    sleep(4000)
    import(url)
}
