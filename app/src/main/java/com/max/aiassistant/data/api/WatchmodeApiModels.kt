package com.max.aiassistant.data.api

import com.google.gson.annotations.SerializedName
import java.time.LocalDate
import java.time.ZoneOffset

data class WatchmodeListTitlesResponse(
    @SerializedName("titles") val titles: List<WatchmodeTitleSummary>? = null,
    @SerializedName("page") val page: Int? = null,
    @SerializedName("total_pages") val totalPages: Int? = null,
    @SerializedName("total_results") val totalResults: Int? = null
)

data class WatchmodeTitleSummary(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("year") val year: Int? = null,
    @SerializedName("type") val type: String? = null
)

data class WatchmodeTitleDetails(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("plot_overview") val plotOverview: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("year") val year: Int? = null,
    @SerializedName("release_date") val releaseDate: String? = null,
    @SerializedName("user_rating") val userRating: Double? = null,
    @SerializedName("critic_score") val criticScore: Int? = null,
    @SerializedName("poster") val poster: String? = null,
    @SerializedName("posterMedium") val posterMedium: String? = null,
    @SerializedName("posterLarge") val posterLarge: String? = null
)

data class WatchmodePlatform(
    val sourceId: String,
    val platformId: String,
    val platformName: String
)

fun WatchmodeTitleDetails.toStreamingRelease(
    platform: WatchmodePlatform,
    frenchOverview: String? = null
): StreamingRelease? {
    val watchmodeId = id ?: return null
    val cleanTitle = title?.trim().orEmpty()
    if (cleanTitle.isBlank()) return null

    return StreamingRelease(
        id = "${watchmodeId}_${platform.platformId}",
        title = cleanTitle,
        overview = frenchOverview?.trim() ?: plotOverview.orEmpty(),
        showType = if (type == "tv_series") "series" else "movie",
        platformId = platform.platformId,
        platformName = platform.platformName,
        addedAt = releaseDate.toIsoDateOrEmpty(),
        releaseYear = year,
        rating = criticScore ?: userRating?.times(10)?.toInt(),
        posterUrl = posterMedium ?: poster ?: posterLarge ?: "",
        link = "https://www.watchmode.com/title/$watchmodeId/"
    )
}

fun WatchmodeTitleSummary.toStreamingRelease(platform: WatchmodePlatform): StreamingRelease? {
    val watchmodeId = id ?: return null
    val cleanTitle = title?.trim().orEmpty()
    if (cleanTitle.isBlank()) return null

    return StreamingRelease(
        id = "${watchmodeId}_${platform.platformId}",
        title = cleanTitle,
        overview = "",
        showType = if (type == "tv_series") "series" else "movie",
        platformId = platform.platformId,
        platformName = platform.platformName,
        addedAt = year?.let { "$it-01-01T00:00:00Z" }.orEmpty(),
        releaseYear = year,
        rating = null,
        posterUrl = "",
        link = "https://www.watchmode.com/title/$watchmodeId/"
    )
}

private fun String?.toIsoDateOrEmpty(): String {
    if (isNullOrBlank()) return ""
    return try {
        LocalDate.parse(this).atStartOfDay().toInstant(ZoneOffset.UTC).toString()
    } catch (_: Exception) {
        this
    }
}
