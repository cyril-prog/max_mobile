package com.max.aiassistant.data.api

import com.google.gson.annotations.SerializedName

/**
 * Modèles pour l'endpoint POST /webhook/get_actu
 *
 * Format de réponse :
 * {
 *   "response": {
 *     "actu": [ { type, origine, description, date_actualite, score, URL, id, createdAt, updatedAt } ],
 *     "recherche": [ { ... même structure, peut être vide ou contenir des objets vides } ]
 *   }
 * }
 */

// ─── Réponse racine ────────────────────────────────────────────────────────────

data class ActuApiResponse(
    @SerializedName("response") val response: ActuResponseBody? = null
)

data class ActuResponseBody(
    @SerializedName("actu") val actu: List<ActuItem>? = null,
    @SerializedName("recherche") val recherche: List<RechercheItem>? = null
)

// ─── Article individuel (onglet "Actualité du jour") ──────────────────────────

data class ActuItem(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("origine") val origine: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("date_actualite") val dateActualite: String? = null,
    @SerializedName("score") val score: String? = null,
    @SerializedName("URL") val url: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

// ─── Article individuel (onglet "Recherche IA") ────────────────────────────────

data class RechercheItem(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("Titre") val titre: String? = null,
    @SerializedName("URL") val url: String? = null,
    @SerializedName("Resume") val resume: String? = null,
    @SerializedName("publication") val publication: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

// ─── Modèle domaine — Actualité ────────────────────────────────────────────────

/**
 * Article d'actualité converti en modèle domaine, prêt pour l'affichage.
 * Le champ [description] de l'API contient "Titre : ...\nDescription : ..." ou du HTML Google News.
 * On extrait le titre et la description propre ici.
 */
data class ActuArticle(
    val id: Int,
    val titre: String,
    val description: String,
    val origine: String,
    val dateActualite: String,
    val score: Int,
    val url: String
)

// ─── Modèle domaine — Recherche IA ─────────────────────────────────────────────

/**
 * Résultat de recherche IA converti en modèle domaine.
 * Champs API : Titre, URL, Resume, publication.
 */
data class RechercheArticle(
    val id: Int,
    val titre: String,
    val resume: String,
    val url: String,
    val publication: String
)

/**
 * Convertit un [ActuItem] brut en [ActuArticle] domaine.
 * - Extrait le titre depuis le champ description ("Titre : ...")
 * - Nettoie les balises HTML présentes dans certains flux (Google News, Marktechpost, The Verge)
 */
fun ActuItem.toActuArticle(): ActuArticle? {
    // Ignore les objets vides (recherche vide = liste avec un objet {})
    if (id == null && description == null && url == null) return null

    val rawDescription = description ?: ""

    // Extraction du titre : "Titre : <titre>\nDescription : <desc>" ou HTML <a href>Titre</a>
    val titre: String
    val desc: String

    when {
        rawDescription.contains("Titre : ") -> {
            // Format texte : "Titre : ...\nDescription : ..."
            val titreMatch = Regex("Titre\\s*:\\s*(.+?)(?:\\n|Description\\s*:)", RegexOption.DOT_MATCHES_ALL)
                .find(rawDescription)
            val descMatch = Regex("Description\\s*:\\s*(.+)", RegexOption.DOT_MATCHES_ALL)
                .find(rawDescription)

            titre = titreMatch?.groupValues?.get(1)?.trim()?.stripHtml() ?: rawDescription.take(80)
            desc = descMatch?.groupValues?.get(1)?.trim()?.stripHtml() ?: ""
        }
        else -> {
            // HTML brut (Google News, Marktechpost, The Verge) : on extrait tout le texte
            val clean = rawDescription.stripHtml()
            // Première ligne = titre (souvent le lien <a>)
            val lines = clean.lines().map { it.trim() }.filter { it.isNotEmpty() }
            titre = lines.firstOrNull() ?: ""
            desc = lines.drop(1).joinToString(" ")
        }
    }

    return ActuArticle(
        id = id ?: 0,
        titre = titre.ifEmpty { origine ?: "Article" },
        description = desc,
        origine = origine ?: "Inconnu",
        dateActualite = dateActualite ?: "",
        score = score?.toIntOrNull() ?: 0,
        url = url ?: ""
    )
}

/**
 * Convertit un [RechercheItem] brut en [RechercheArticle] domaine.
 * Ignore les objets vides (id == null && resume == null).
 */
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

/**
 */
private fun String.stripHtml(): String {
    return this
        .replace(Regex("<[^>]+>"), "") // supprime les balises
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#8217;", "'")
        .replace("&#8216;", "'")
        .replace("&#8230;", "…")
        .replace("&nbsp;", " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}
