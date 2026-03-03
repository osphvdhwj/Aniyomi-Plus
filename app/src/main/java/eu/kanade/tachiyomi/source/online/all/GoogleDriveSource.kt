package eu.kanade.tachiyomi.source.online.all

import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.data.sync.service.GoogleDriveSyncService
import eu.kanade.tachiyomi.network.GET
import okhttp3.Request
import okhttp3.Response
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * A source that treats a Google Drive folder as an anime library.
 * Currently a stub implementation to integrate with the sync service.
 */
class GoogleDriveSource : AnimeHttpSource() {

    override val name = "Google Drive (Cloud)"
    override val baseUrl = "https://www.googleapis.com/drive/v3"
    override val lang = "all"
    override val supportsLatest = true

    private val driveService: GoogleDriveSyncService by lazy { Injekt.get() }

    // This would need a way to list files from the drive service
    // For now, return a placeholder to show it exists in the source list
    override fun fetchPopularAnime(page: Int): rx.Observable<AnimesPage> {
        return rx.Observable.just(AnimesPage(emptyList(), false))
    }

    override fun fetchSearchAnime(page: Int, query: String, filters: AnimeFilterList): rx.Observable<AnimesPage> {
        return rx.Observable.just(AnimesPage(emptyList(), false))
    }

    override fun fetchLatestUpdates(page: Int): rx.Observable<AnimesPage> {
        return rx.Observable.just(AnimesPage(emptyList(), false))
    }

    override fun fetchAnimeDetails(anime: SAnime): rx.Observable<SAnime> {
        return rx.Observable.just(anime)
    }

    override fun fetchEpisodeList(anime: SAnime): rx.Observable<List<SEpisode>> {
        return rx.Observable.just(emptyList())
    }

    override fun fetchVideoList(episode: SEpisode): rx.Observable<List<Video>> {
        return rx.Observable.just(emptyList())
    }

    override fun popularAnimeRequest(page: Int): Request = GET("$baseUrl/files")
    override fun popularAnimeParse(response: Response): AnimesPage = AnimesPage(emptyList(), false)
    override fun latestUpdatesRequest(page: Int): Request = GET("$baseUrl/files?orderBy=createdTime desc")
    override fun latestUpdatesParse(response: Response): AnimesPage = AnimesPage(emptyList(), false)
    override fun searchAnimeRequest(
        page: Int,
        query: String,
        filters: AnimeFilterList,
    ): Request = GET("$baseUrl/files?q=name contains '$query'")
    override fun searchAnimeParse(response: Response): AnimesPage = AnimesPage(emptyList(), false)
    override fun animeDetailsParse(response: Response): SAnime = SAnime.create()
    override fun episodeListParse(response: Response): List<SEpisode> = emptyList()
    override fun videoListParse(response: Response): List<Video> = emptyList()
}
