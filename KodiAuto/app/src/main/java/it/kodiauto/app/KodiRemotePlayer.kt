package it.kodiauto.app

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.SimpleBasePlayer
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import org.json.JSONObject
import java.util.concurrent.ExecutorService

/**
 * Media3 Player facade for Kodi's real playback engine.
 * Android Auto controls are forwarded to Kodi JSON-RPC; Kodi remains the player.
 */
@OptIn(UnstableApi::class)
internal class KodiRemotePlayer(
    private val worker: ExecutorService
) : SimpleBasePlayer(android.os.Looper.getMainLooper()) {

    @Volatile private var currentItem: MediaItem? = null
    @Volatile private var playing = false
    @Volatile private var positionMs = 0L
    @Volatile private var durationMs = C.TIME_UNSET
    @Volatile private var kodiPlayerId = 1

    private val commands = Player.Commands.Builder()
        .addAll(
            Player.COMMAND_PLAY_PAUSE,
            Player.COMMAND_STOP,
            Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_NEXT,
            Player.COMMAND_SEEK_TO_PREVIOUS,
            Player.COMMAND_SET_MEDIA_ITEM,
            Player.COMMAND_CHANGE_MEDIA_ITEMS,
            Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
            Player.COMMAND_GET_METADATA,
            Player.COMMAND_GET_TIMELINE
        )
        .build()

    override fun getState(): State {
        val item = currentItem
        val playlist = if (item == null) {
            emptyList()
        } else {
            listOf(
                SimpleBasePlayer.MediaItemData.Builder(item.mediaId)
                    .setMediaItem(item)
                    .setDurationUs(if (durationMs > 0) durationMs * 1000 else C.TIME_UNSET)
                    .setIsSeekable(true)
                    .build()
            )
        }
        return State.Builder()
            .setAvailableCommands(commands)
            .setPlaybackState(if (item == null) Player.STATE_IDLE else Player.STATE_READY)
            .setPlayWhenReady(playing, Player.PLAY_WHEN_READY_CHANGE_REASON_REMOTE)
            .setPlaylist(playlist)
            .setCurrentMediaItemIndex(if (item == null) C.INDEX_UNSET else 0)
            .setContentPositionMs(positionMs)
            .build()
    }

    override fun handleSetMediaItems(
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long
    ): ListenableFuture<*> = async {
        val selected = mediaItems.getOrNull(startIndex.coerceAtLeast(0))
            ?: throw IllegalArgumentException("Nessun contenuto Kodi selezionato")
        when {
            selected.mediaId.startsWith("song:") ->
                KodiJsonRpcClient.openSong(selected.mediaId.removePrefix("song:").toInt())
            selected.mediaId.startsWith("movie:") ->
                KodiJsonRpcClient.openMovie(selected.mediaId.removePrefix("movie:").toInt())
            selected.mediaId.startsWith("episode:") ->
                KodiJsonRpcClient.openEpisode(selected.mediaId.removePrefix("episode:").toInt())
            selected.mediaId.startsWith("channel:") ->
                KodiJsonRpcClient.openChannel(selected.mediaId.removePrefix("channel:").toInt())
            else -> throw IllegalArgumentException("Tipo di contenuto Kodi non supportato")
        }
        // Kodi uses different player IDs for audio and video. Read the active
        // player immediately so the first transport command reaches the right one.
        val opened = KodiJsonRpcClient.playbackState()
        kodiPlayerId = opened.optInt("playerid", kodiPlayerId)
        currentItem = selected
        positionMs = startPositionMs.coerceAtLeast(0)
        durationMs = C.TIME_UNSET
        playing = true
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> = async {
        KodiJsonRpcClient.setPlaying(playWhenReady, kodiPlayerId)
        playing = playWhenReady
    }

    override fun handleStop(): ListenableFuture<*> = async {
        KodiJsonRpcClient.stop(kodiPlayerId)
        playing = false
        currentItem = null
        positionMs = 0
        durationMs = C.TIME_UNSET
    }

    override fun handleSeek(
        mediaItemIndex: Int,
        positionMs: Long,
        seekCommand: Int
    ): ListenableFuture<*> = async {
        when (seekCommand) {
            Player.COMMAND_SEEK_TO_NEXT -> KodiJsonRpcClient.goTo("next", kodiPlayerId)
            Player.COMMAND_SEEK_TO_PREVIOUS -> KodiJsonRpcClient.goTo("previous", kodiPlayerId)
            else -> KodiJsonRpcClient.seek(positionMs, kodiPlayerId)
        }
        if (positionMs >= 0) this.positionMs = positionMs
    }

    /** Called by the service's Kodi poller on the main looper. */
    fun updateFromKodi(snapshot: JSONObject) {
        if (!snapshot.optBoolean("active", false)) {
            currentItem = null
            playing = false
            positionMs = 0
            durationMs = C.TIME_UNSET
            invalidateState()
            return
        }

        kodiPlayerId = snapshot.optInt("playerid", kodiPlayerId)
        val properties = snapshot.optJSONObject("properties") ?: JSONObject()
        val item = snapshot.optJSONObject("item") ?: JSONObject()
        val type = item.optString("type")
        val id = item.optInt("id", -1)
        val mediaId = when (type) {
            "song" -> if (id >= 0) "song:$id" else "playing:$type"
            "movie" -> if (id >= 0) "movie:$id" else "playing:$type"
            "episode" -> if (id >= 0) "episode:$id" else "playing:$type"
            "channel" -> if (id >= 0) "channel:$id" else "playing:$type"
            else -> "playing:$type"
        }
        val metadata = MediaMetadata.Builder()
            .setTitle(item.optString("title", "Kodi Auto"))
            .setArtist(item.optJSONArray("artist")?.optString(0))
            .setAlbumTitle(item.optString("album", ""))
            .setSubtitle(item.optString("year", ""))
            .build()
        currentItem = MediaItem.Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(metadata)
            .build()

        val time = properties.optJSONObject("time") ?: JSONObject()
        val total = properties.optJSONObject("totaltime") ?: JSONObject()
        positionMs = timeToMs(time)
        durationMs = timeToMs(total).takeIf { it > 0 } ?: C.TIME_UNSET
        playing = properties.optInt("speed", 0) != 0
        invalidateState()
    }

    private fun timeToMs(time: JSONObject): Long =
        time.optLong("hours") * 3_600_000L +
            time.optLong("minutes") * 60_000L +
            time.optLong("seconds") * 1_000L +
            time.optLong("milliseconds")

    private fun async(block: () -> Unit): ListenableFuture<*> {
        val future = SettableFuture.create<Any>()
        worker.execute {
            try {
                block()
                future.set(Unit)
            } catch (error: Throwable) {
                future.setException(error)
            }
        }
        return future
    }
}
