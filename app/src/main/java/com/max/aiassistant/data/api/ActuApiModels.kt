package com.max.aiassistant.data.api

import com.google.gson.annotations.SerializedName

/**
 * Models for POST /webhook/get_actu.
 */
data class ActuApiResponse(
    @SerializedName("response") val response: ActuResponseBody? = null
)

data class ActuResponseBody(
    @SerializedName("actu") val actu: List<ActuItem>? = null,
    @SerializedName("recherche") val recherche: List<RechercheItem>? = null
)

data class ActuItem(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("origine") val origine: String? = null,
    @SerializedName(value = "titre", alternate = ["Titre"]) val titre: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("date_actualite") val dateActualite: String? = null,
    @SerializedName("score") val score: String? = null,
    @SerializedName("URL") val url: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

data class RechercheItem(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("Titre") val titre: String? = null,
    @SerializedName("URL") val url: String? = null,
    @SerializedName("Resume") val resume: String? = null,
    @SerializedName("publication") val publication: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

data class ActuArticle(
    val id: Int,
    val titre: String,
    val description: String,
    val origine: String,
    val dateActualite: String,
    val score: Int,
    val url: String
)

data class RechercheArticle(
    val id: Int,
    val titre: String,
    val resume: String,
    val url: String,
    val publication: String
)

fun ActuItem.toActuArticle(): ActuArticle? {
    if (id == null && titre == null && description == null && url == null) return null

    val rawTitre = titre?.stripHtml().orEmpty()
    val rawDescription = description.orEmpty()
    val (legacyTitre, legacyDescription) = rawDescription.extractLegacyActuContent()
    val finalTitre = rawTitre.ifBlank { legacyTitre }.ifBlank { origine ?: "Article" }
    val finalDescription = rawDescription.extractDescriptionOnly(finalTitre).ifBlank { legacyDescription }

    return ActuArticle(
        id = id ?: 0,
        titre = finalTitre,
        description = finalDescription,
        origine = origine ?: "Inconnu",
        dateActualite = dateActualite ?: "",
        score = score?.toIntOrNull() ?: 0,
        url = url ?: ""
    )
}

fun RechercheItem.toRechercheArticle(): RechercheArticle? {
    if (id == null && resume == null && url == null) return null
    return RechercheArticle(
        id = id ?: 0,
        titre = titre ?: "",
        resume = resume ?: "",
        url = url ?: "",
        publication = publication ?: ""
    )
}

private fun String.extractLegacyActuContent(): Pair<String, String> {
    if (isBlank()) return "" to ""

    if (contains("Titre : ")) {
        val titreMatch = Regex("Titre\\s*:\\s*(.+?)(?:\\n|Description\\s*:)", RegexOption.DOT_MATCHES_ALL)
            .find(this)
        val descriptionMatch = Regex("Description\\s*:\\s*(.+)", RegexOption.DOT_MATCHES_ALL)
            .find(this)

        return (
            titreMatch?.groupValues?.getOrNull(1)?.trim()?.stripHtml().orEmpty() to
                descriptionMatch?.groupValues?.getOrNull(1)?.trim()?.stripHtml().orEmpty()
            )
    }

    val clean = stripHtml()
    val lines = clean.lines().map { it.trim() }.filter { it.isNotEmpty() }
    return lines.firstOrNull().orEmpty() to lines.drop(1).joinToString(" ")
}

private fun String.extractDescriptionOnly(fallbackTitle: String): String {
    if (isBlank()) return ""

    val legacyDescription = Regex("Description\\s*:\\s*(.+)", RegexOption.DOT_MATCHES_ALL)
        .find(this)
        ?.groupValues
        ?.getOrNull(1)
        ?.trim()
        ?.stripHtml()
    if (!legacyDescription.isNullOrBlank()) return legacyDescription

    val clean = stripHtml()
    if (clean.isBlank()) return ""

    return clean
        .removePrefix(fallbackTitle)
        .trim()
        .trimStart(':', '-', ' ', '|')
        .trim()
}

private fun String.stripHtml(): String {
    return this
        .replace(Regex("<[^>]+>"), "")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#8217;", "'")
        .replace("&#8216;", "'")
        .replace("&#8230;", "...")
        .replace("&nbsp;", " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}
