package it.kodiauto.app

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import java.util.concurrent.Executors

/**
 * Android Auto library backed by the separate Kodi engine through local JSON-RPC.
 * Browsing is connected in this step; remote playback/session synchronisation follows.
 */
class KodiAutoMediaLibraryService : MediaLibraryService() {

    private var librarySession: MediaLibrarySession? = null
    private val worker = Executors.newSingleThreadExecutor()

    private val libraryCallback = object : MediaLibrarySession.Callback {
        override fun onSetMediaItems(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ): ListenableFuture<List<MediaItem>> {
            val future = SettableFuture.create<List<MediaItem>>()
            worker.execute {
                try {
                    val selected = mediaItems.getOrNull(startIndex)
                        ?: throw IllegalArgumentException("Nessun contenuto selezionato")
                    when {
                        selected.mediaId.startsWith("song:") ->
                            KodiJsonRpcClient.openSong(selected.mediaId.removePrefix("song:").toInt())
                        selected.mediaId.startsWith("movie:") ->
                            KodiJsonRpcClient.openMovie(selected.mediaId.removePrefix("movie:").toInt())
                        else -> throw IllegalArgumentException("Elemento non riproducibile")
                    }
                    future.set(mediaItems)
                } catch (error: Exception) {
                    future.setException(error)
                }
            }
            return future
        }

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
                            folder(VIDEO_ID, "Video", true)
                        )
                        parentId == MUSIC_ID -> loadAlbums()
                        parentId == VIDEO_ID -> loadMovies()
                        parentId.startsWith(ALBUM_PREFIX) -> loadSongs(parentId.removePrefix(ALBUM_PREFIX).toInt())
                        else -> emptyList()
                    }
                    val from = (page * pageSize).coerceIn(0, children.size)
                    val to = (from + pageSize).coerceIn(from, children.size)
                    future.set(LibraryResult.ofItemList(ImmutableList.copyOf(children.subList(from, to)), params))
                } catch (error: Exception) {
                    val hint = if (parentId == ROOT_ID) {
                        "Avvia Kodi Auto e abilita il controllo locale in Impostazioni > Servizi > Controllo."
                    } else {
                        "Libreria Kodi non disponibile. Verifica che Kodi sia aperto e che il controllo locale sia attivo."
                    }
                    val item = MediaItem.Builder()
                        .setMediaId("kodi_connection_error")
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle("Kodi non raggiungibile")
                                .setSubtitle(hint)
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
        val player = ExoPlayer.Builder(this).build()
        librarySession = MediaLibrarySession.Builder(this, player, libraryCallback).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return librarySession
    }

    override fun onDestroy() {
        librarySession?.let { session ->
            session.player.release()
            session.release()
        }
        librarySession = null
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
                    val artist = album.optJSONArray("artist")?.optString(0).orEmpty()
                    add(
                        MediaItem.Builder()
                            .setMediaId(ALBUM_PREFIX + id)
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(album.optString("title", "Album"))
                                    .setArtist(artist)
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
                                    .setTitle(movie.optString("title", "Video"))
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
        const val VIDEO_ID = "kodiauto_video"
        const val ALBUM_PREFIX = "album:"
    }
}
