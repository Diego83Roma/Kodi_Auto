package it.kodiauto.app

import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import java.util.concurrent.Executors

/**
 * Android Auto media browser backed by Kodi's own library and playback engine.
 * The Media3 player is a remote-control facade: Kodi performs the actual playback.
 */
@OptIn(UnstableApi::class)
class KodiAutoMediaLibraryService : MediaLibraryService() {

    private val worker = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var kodiPlayer: KodiRemotePlayer? = null
    private var librarySession: MediaLibrarySession? = null

    private val pollKodi = object : Runnable {
        override fun run() {
            if (librarySession == null) return
            worker.execute {
                val snapshot = try {
                    KodiJsonRpcClient.playbackState()
                } catch (_: Exception) {
                    null
                }
                mainHandler.post {
                    if (snapshot != null) kodiPlayer?.updateFromKodi(snapshot)
                    if (librarySession != null) mainHandler.postDelayed(this, 1500)
                }
            }
        }
    }

    private val libraryCallback = object : MediaLibrarySession.Callback {
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val root = folder(ROOT_ID, "Kodi Auto", true)
            return Futures.immediateFuture(LibraryResult.ofItem(root, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
            worker.execute {
                try {
                    val children = when {
                        parentId == ROOT_ID -> listOf(
                            folder(MUSIC_ID, "Musica", true),
                            folder(MOVIES_ID, "Film", true),
                            folder(TV_ID, "Serie TV", true),
                            folder(LIVE_ID, "TV in diretta", true)
                        )
                        parentId == MUSIC_ID -> loadAlbums()
                        parentId == MOVIES_ID -> loadMovies()
                        parentId == TV_ID -> loadTvShows()
                        parentId == LIVE_ID -> loadChannels()
                        parentId.startsWith(ALBUM_PREFIX) ->
                            loadSongs(parentId.removePrefix(ALBUM_PREFIX).toInt())
                        parentId.startsWith(TVSHOW_PREFIX) ->
                            loadEpisodes(parentId.removePrefix(TVSHOW_PREFIX).toInt())
                        else -> emptyList()
                    }
                    val from = (page * pageSize).coerceIn(0, children.size)
                    val to = (from + pageSize).coerceIn(from, children.size)
                    future.set(
                        LibraryResult.ofItemList(
                            ImmutableList.copyOf(children.subList(from, to)),
                            params
                        )
                    )
                } catch (_: Exception) {
                    val item = MediaItem.Builder()
                        .setMediaId("kodi_connection_error")
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle("Kodi non raggiungibile")
                                .setSubtitle(
                                    "Apri Kodi Auto e abilita il controllo locale in Impostazioni > Servizi > Controllo."
                                )
                                .setIsBrowsable(false)
                                .setIsPlayable(false)
                                .build()
                        )
                        .build()
                    future.set(LibraryResult.ofItemList(ImmutableList.of(item), params))
                }
            }
            return future
        }
    }

    override fun onCreate() {
        super.onCreate()
        kodiPlayer = KodiRemotePlayer(worker)
        librarySession = MediaLibrarySession.Builder(this, kodiPlayer!!, libraryCallback).build()
        mainHandler.post(pollKodi)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
        librarySession

    override fun onDestroy() {
        mainHandler.removeCallbacks(pollKodi)
        librarySession?.let { session ->
            session.player.release()
            session.release()
        }
        librarySession = null
        kodiPlayer = null
        worker.shutdownNow()
        super.onDestroy()
    }

    private fun loadAlbums(): List<MediaItem> {
        val albums = KodiJsonRpcClient.albums()
        return buildList {
            for (i in 0 until albums.length()) {
                val album = albums.getJSONObject(i)
                val id = album.optInt("albumid", -1)
                if (id >= 0) {
                    add(
                        MediaItem.Builder()
                            .setMediaId(ALBUM_PREFIX + id)
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(album.optString("title", "Album"))
                                    .setArtist(album.optJSONArray("artist")?.optString(0).orEmpty())
                                    .setIsBrowsable(true)
                                    .setIsPlayable(false)
                                    .build()
                            )
                            .build()
                    )
                }
            }
        }
    }

    private fun loadSongs(albumId: Int): List<MediaItem> {
        val songs = KodiJsonRpcClient.songs(albumId)
        return buildList {
            for (i in 0 until songs.length()) {
                val song = songs.getJSONObject(i)
                val id = song.optInt("songid", -1)
                if (id >= 0) {
                    add(
                        MediaItem.Builder()
                            .setMediaId("song:$id")
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(song.optString("title", "Brano"))
                                    .setArtist(song.optJSONArray("artist")?.optString(0).orEmpty())
                                    .setAlbumTitle(song.optString("album", ""))
                                    .setIsBrowsable(false)
                                    .setIsPlayable(true)
                                    .build()
                            )
                            .build()
                    )
                }
            }
        }
    }

    private fun loadMovies(): List<MediaItem> {
        val movies = KodiJsonRpcClient.movies()
        return buildList {
            for (i in 0 until movies.length()) {
                val movie = movies.getJSONObject(i)
                val id = movie.optInt("movieid", -1)
                if (id >= 0) {
                    add(
                        MediaItem.Builder()
                            .setMediaId("movie:$id")
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(movie.optString("title", "Film"))
                                    .setSubtitle(movie.optString("year", ""))
                                    .setIsBrowsable(false)
                                    .setIsPlayable(true)
                                    .build()
                            )
                            .build()
                    )
                }
            }
        }
    }

    private fun loadTvShows(): List<MediaItem> {
        val shows = KodiJsonRpcClient.tvShows()
        return buildList {
            for (i in 0 until shows.length()) {
                val show = shows.getJSONObject(i)
                val id = show.optInt("tvshowid", -1)
                if (id >= 0) {
                    add(
                        MediaItem.Builder()
                            .setMediaId(TVSHOW_PREFIX + id)
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(show.optString("title", "Serie TV"))
                                    .setSubtitle(show.optString("year", ""))
                                    .setIsBrowsable(true)
                                    .setIsPlayable(false)
                                    .build()
                            )
                            .build()
                    )
                }
            }
        }
    }

    private fun loadEpisodes(tvShowId: Int): List<MediaItem> {
        val episodes = KodiJsonRpcClient.episodes(tvShowId)
        return buildList {
            for (i in 0 until episodes.length()) {
                val episode = episodes.getJSONObject(i)
                val id = episode.optInt("episodeid", -1)
                if (id >= 0) {
                    val season = episode.optInt("season", 0)
                    val number = episode.optInt("episode", 0)
                    add(
                        MediaItem.Builder()
                            .setMediaId("episode:$id")
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(episode.optString("title", "Episodio"))
                                    .setSubtitle("Stagione $season · Episodio $number")
                                    .setIsBrowsable(false)
                                    .setIsPlayable(true)
                                    .build()
                            )
                            .build()
                    )
                }
            }
        }
    }

    private fun loadChannels(): List<MediaItem> {
        val channels = KodiJsonRpcClient.channels()
        return buildList {
            for (i in 0 until channels.length()) {
                val channel = channels.getJSONObject(i)
                val id = channel.optInt("channelid", -1)
                if (id >= 0) {
                    add(
                        MediaItem.Builder()
                            .setMediaId("channel:$id")
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(channel.optString("label", "Canale TV"))
                                    .setIsBrowsable(false)
                                    .setIsPlayable(true)
                                    .build()
                            )
                            .build()
                    )
                }
            }
        }
    }

    private fun folder(id: String, title: String, browsable: Boolean): MediaItem =
        MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setIsBrowsable(browsable)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

    private companion object {
        const val ROOT_ID = "kodiauto_root"
        const val MUSIC_ID = "kodiauto_music"
        const val MOVIES_ID = "kodiauto_movies"
        const val TV_ID = "kodiauto_tv"
        const val LIVE_ID = "kodiauto_live"
        const val ALBUM_PREFIX = "album:"
        const val TVSHOW_PREFIX = "tvshow:"
    }
}
