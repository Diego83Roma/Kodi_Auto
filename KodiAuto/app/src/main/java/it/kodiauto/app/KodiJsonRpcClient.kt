package it.kodiauto.app

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicInteger

/**
 * Local JSON-RPC bridge to the separate Kodi engine app.
 * Kodi must be running and allow control from applications on this device.
 */
internal object KodiJsonRpcClient {
    private const val TAG = "KodiJsonRpc"
    private const val HOST = "127.0.0.1"
    private const val PORT = 9090
    private val ids = AtomicInteger(1)

    fun call(method: String, params: JSONObject = JSONObject()): JSONObject {
        val id = ids.getAndIncrement()
        val request = JSONObject()
            .put("jsonrpc", "2.0")
            .put("method", method)
            .put("params", params)
            .put("id", id)

        Socket().use { socket ->
            socket.connect(InetSocketAddress(HOST, PORT), 1500)
            socket.soTimeout = 3500
            val writer = OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8)
            writer.write(request.toString())
            writer.flush()
            val response = readJsonObject(socket.getInputStream())
            val json = JSONObject(response)
            if (json.has("error")) {
                throw IllegalStateException(json.getJSONObject("error").optString("message", "Errore JSON-RPC"))
            }
            return json.optJSONObject("result") ?: JSONObject()
        }
    }

    fun openSong(songId: Int) {
        call("Player.Open", JSONObject().put("item", JSONObject().put("songid", songId)))
    }

    fun openMovie(movieId: Int) {
        call("Player.Open", JSONObject().put("item", JSONObject().put("movieid", movieId)))
    }

    fun openEpisode(episodeId: Int) {
        call("Player.Open", JSONObject().put("item", JSONObject().put("episodeid", episodeId)))
    }

    fun openChannel(channelId: Int) {
        call("Player.Open", JSONObject().put("item", JSONObject().put("channelid", channelId)))
    }

    fun setPlaying(playing: Boolean, playerId: Int) {
        call("Player.PlayPause", JSONObject().put("playerid", playerId).put("play", playing))
    }

    fun stop(playerId: Int) {
        call("Player.Stop", JSONObject().put("playerid", playerId))
    }

    fun goTo(direction: String, playerId: Int) {
        call("Player.GoTo", JSONObject().put("playerid", playerId).put("to", direction))
    }

    fun seek(positionMs: Long, playerId: Int) {
        val totalMs = positionMs.coerceAtLeast(0)
        val time = JSONObject()
            .put("hours", totalMs / 3_600_000)
            .put("minutes", (totalMs % 3_600_000) / 60_000)
            .put("seconds", (totalMs % 60_000) / 1_000)
            .put("milliseconds", totalMs % 1_000)
        call("Player.Seek", JSONObject().put("playerid", playerId).put("value", JSONObject().put("time", time)))
    }

    fun playbackState(): JSONObject {
        val active = call("Player.GetActivePlayers").optJSONArray("players") ?: JSONArray()
        if (active.length() == 0) return JSONObject().put("active", false)
        val playerId = active.getJSONObject(0).optInt("playerid", 1)
        val properties = call(
            "Player.GetProperties",
            JSONObject()
                .put("playerid", playerId)
                .put("properties", JSONArray()
                    .put("time")
                    .put("totaltime")
                    .put("speed")
                    .put("percentage")
                    .put("canseek"))
        )
        val itemResult = call(
            "Player.GetItem",
            JSONObject()
                .put("playerid", playerId)
                .put("properties", JSONArray()
                    .put("title")
                    .put("artist")
                    .put("album")
                    .put("year")
                    .put("thumbnail")
                    .put("type")
                    .put("id"))
        )
        return JSONObject()
            .put("active", true)
            .put("playerid", playerId)
            .put("properties", properties)
            .put("item", itemResult.optJSONObject("item") ?: JSONObject())
    }

    fun albums(): JSONArray {
        val params = JSONObject()
            .put("properties", JSONArray().put("title").put("artist").put("year").put("thumbnail"))
            .put("limits", JSONObject().put("start", 0).put("end", 200))
        return call("AudioLibrary.GetAlbums", params).optJSONArray("albums") ?: JSONArray()
    }

    fun songs(albumId: Int): JSONArray {
        val params = JSONObject()
            .put("albumid", albumId)
            .put("properties", JSONArray().put("title").put("artist").put("album").put("duration").put("file"))
            .put("limits", JSONObject().put("start", 0).put("end", 300))
        return call("AudioLibrary.GetSongs", params).optJSONArray("songs") ?: JSONArray()
    }

    fun movies(): JSONArray {
        val params = JSONObject()
            .put("properties", JSONArray().put("title").put("year").put("thumbnail").put("file").put("runtime"))
            .put("limits", JSONObject().put("start", 0).put("end", 200))
        return call("VideoLibrary.GetMovies", params).optJSONArray("movies") ?: JSONArray()
    }

    private fun readJsonObject(input: java.io.InputStream): String {
        val reader = BufferedReader(InputStreamReader(input, Charsets.UTF_8))
        val out = StringBuilder()
        var depth = 0
        var inString = false
        var escaped = false
        while (true) {
            val value = reader.read()
            if (value == -1) throw IllegalStateException("Kodi ha chiuso la connessione JSON-RPC")
            val ch = value.toChar()
            if (depth == 0 && ch != '{') continue
            out.append(ch)
            if (inString) {
                if (escaped) escaped = false
                else if (ch == '\\') escaped = true
                else if (ch == '"') inString = false
            } else {
                when (ch) {
                    '"' -> inString = true
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) return out.toString()
                    }
                }
            }
        }
    }
}
