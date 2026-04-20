package com.max.aiassistant.data.api

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.TimeUnit

interface PollenInformationApiService {

    @GET("api/forecast/public")
    suspend fun lookupForecast(
        @Query("country") countryCode: String,
        @Query("lang") languageCode: String = "fr",
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("apikey") apiKey: String
    ): PollenInformationForecastResponse

    companion object {
        private const val BASE_URL = "https://www.polleninformation.at/"

        fun create(): PollenInformationApiService {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(PollenInformationApiService::class.java)
        }
    }
}

data class PollenInformationForecastResponse(
    val contamination: List<PollenInformationContamination> = emptyList(),
    val allergyrisk: PollenInformationAllergyRisk? = null,
    @SerializedName("allergyrisk_hourly")
    val allergyRiskHourly: PollenInformationAllergyRiskHourly? = null,
    val error: String? = null
)

data class PollenInformationContamination(
    @SerializedName("poll_id")
    val pollId: Int,
    @SerializedName("poll_title")
    val pollTitle: String,
    @SerializedName("contamination_1")
    val contaminationToday: Int? = null,
    @SerializedName("contamination_2")
    val contaminationTomorrow: Int? = null,
    @SerializedName("contamination_3")
    val contaminationDay3: Int? = null,
    @SerializedName("contamination_4")
    val contaminationDay4: Int? = null
)

data class PollenInformationAllergyRisk(
    @SerializedName("allergyrisk_1")
    val today: Int? = null,
    @SerializedName("allergyrisk_2")
    val tomorrow: Int? = null,
    @SerializedName("allergyrisk_3")
    val day3: Int? = null,
    @SerializedName("allergyrisk_4")
    val day4: Int? = null
)

data class PollenInformationAllergyRiskHourly(
    @SerializedName("allergyrisk_hourly_1")
    val today: List<Int> = emptyList(),
    @SerializedName("allergyrisk_hourly_2")
    val tomorrow: List<Int> = emptyList(),
    @SerializedName("allergyrisk_hourly_3")
    val day3: List<Int> = emptyList(),
    @SerializedName("allergyrisk_hourly_4")
    val day4: List<Int> = emptyList()
)

fun PollenInformationForecastResponse.toCurrentPollenData(): CurrentPollenData? {
    if (!error.isNullOrBlank()) {
        return null
    }

    val plants = contamination.mapNotNull { item ->
        val descriptor = item.toPlantDescriptor() ?: return@mapNotNull null
        PollenPlantData(
            code = descriptor.code,
            displayName = descriptor.displayName,
            plantType = descriptor.plantType,
            indexValue = item.contaminationToday.toAppSeverityIndex(),
            category = item.contaminationToday.toPollenCategoryLabel(),
            inSeason = item.isInSeason()
        )
    }

    if (plants.isEmpty()) {
        return null
    }

    val pollenTypes = plants
        .groupBy { it.plantType }
        .map { (plantType, groupedPlants) ->
            val maxSeverity = groupedPlants.mapNotNull { it.indexValue }.maxOrNull()
            PollenTypeData(
                code = plantType.toPollenTypeCode(),
                displayName = plantType.toPollenTypeDisplayName(),
                indexValue = maxSeverity,
                category = maxSeverity.toPollenCategoryLabel(),
                inSeason = groupedPlants.any { it.inSeason == true }
            )
        }

    return CurrentPollenData(
        grassPollen = plants.firstOrNull { it.code == "GRAMINALES" }?.indexValue?.toDouble(),
        birchPollen = plants.firstOrNull { it.code == "BIRCH" }?.indexValue?.toDouble(),
        alderPollen = plants.firstOrNull { it.code == "ALDER" }?.indexValue?.toDouble(),
        olivePollen = plants.firstOrNull { it.code == "OLIVE" }?.indexValue?.toDouble(),
        mugwortPollen = plants.firstOrNull { it.code == "MUGWORT" }?.indexValue?.toDouble(),
        ragweedPollen = plants.firstOrNull { it.code == "RAGWEED" }?.indexValue?.toDouble(),
        pollenTypes = pollenTypes,
        pollenPlants = plants,
        pollenSource = "polleninformation"
    )
}

private data class PollenInformationPlantDescriptor(
    val code: String,
    val displayName: String,
    val plantType: String
)

private fun PollenInformationContamination.toPlantDescriptor(): PollenInformationPlantDescriptor? {
    val normalizedTitle = pollTitle.normalizePollenLabel()
    val displayName = pollTitle.substringBefore(" (").trim()

    return when {
        normalizedTitle.contains("graminees") || normalizedTitle.contains("poaceae") ->
            PollenInformationPlantDescriptor("GRAMINALES", "Graminées", "GRASS")
        normalizedTitle.contains("bouleau") || normalizedTitle.contains("betula") ->
            PollenInformationPlantDescriptor("BIRCH", "Bouleau", "TREE")
        normalizedTitle.contains("aulne") || normalizedTitle.contains("alnus") ->
            PollenInformationPlantDescriptor("ALDER", "Aulne", "TREE")
        normalizedTitle.contains("olivier") || normalizedTitle.contains("olea") ->
            PollenInformationPlantDescriptor("OLIVE", "Olivier", "TREE")
        normalizedTitle.contains("armoise") || normalizedTitle.contains("artemisia") ->
            PollenInformationPlantDescriptor("MUGWORT", "Armoise", "WEED")
        normalizedTitle.contains("ambroisie") || normalizedTitle.contains("ambrosia") ->
            PollenInformationPlantDescriptor("RAGWEED", "Ambroisie", "WEED")
        normalizedTitle.contains("chene") || normalizedTitle.contains("quercus") ->
            PollenInformationPlantDescriptor("OAK", "Chêne", "TREE")
        normalizedTitle.contains("frene") || normalizedTitle.contains("fraxinus") ->
            PollenInformationPlantDescriptor("ASH", "Frêne", "TREE")
        normalizedTitle.contains("noiset") || normalizedTitle.contains("corylus") ->
            PollenInformationPlantDescriptor("HAZEL", "Noisetier", "TREE")
        normalizedTitle.contains("genevrier") || normalizedTitle.contains("juniper") || normalizedTitle.contains("juniperus") ->
            PollenInformationPlantDescriptor("JUNIPER", "Genévrier", "TREE")
        normalizedTitle.contains("erable") || normalizedTitle.contains("acer") ->
            PollenInformationPlantDescriptor("MAPLE", "Érable", "TREE")
        normalizedTitle.contains("orme") || normalizedTitle.contains("ulmus") ->
            PollenInformationPlantDescriptor("ELM", "Orme", "TREE")
        normalizedTitle.contains("peuplier") || normalizedTitle.contains("populus") ->
            PollenInformationPlantDescriptor("COTTONWOOD", "Peuplier", "TREE")
        normalizedTitle.contains("cypres") || normalizedTitle.contains("cupress") ->
            PollenInformationPlantDescriptor("CYPRESS_PINE", "Cyprès", "TREE")
        normalizedTitle.contains("pin") || normalizedTitle.contains("pinus") ->
            PollenInformationPlantDescriptor("PINE", "Pin", "TREE")
        normalizedTitle.contains("platane") || normalizedTitle.contains("platan") ->
            PollenInformationPlantDescriptor("PLANE", "Platane", "TREE")
        normalizedTitle.contains("saule") || normalizedTitle.contains("salix") ->
            PollenInformationPlantDescriptor("WILLOW", "Saule", "TREE")
        normalizedTitle.contains("tilleul") || normalizedTitle.contains("tilia") ->
            PollenInformationPlantDescriptor("LINDEN", "Tilleul", "TREE")
        normalizedTitle.contains("oseille") || normalizedTitle.contains("rumex") ->
            PollenInformationPlantDescriptor("DOCK", "Oseille", "WEED")
        normalizedTitle.contains("urticace") || normalizedTitle.contains("urtica") ->
            PollenInformationPlantDescriptor("NETTLE", "Urticacées", "WEED")
        normalizedTitle.contains("plantain") || normalizedTitle.contains("plantago") ->
            PollenInformationPlantDescriptor("PLANTAIN", "Plantain", "WEED")
        else -> null
    }
}

private fun PollenInformationContamination.isInSeason(): Boolean {
    return listOf(
        contaminationToday,
        contaminationTomorrow,
        contaminationDay3,
        contaminationDay4
    ).any { (it ?: 0) > 0 }
}

private fun Int?.toAppSeverityIndex(): Int? {
    return when (this) {
        null -> null
        0 -> 0
        else -> this + 1
    }
}

private fun Int?.toPollenCategoryLabel(): String {
    return when (this) {
        null -> "Indisponible"
        0 -> "Aucun"
        1 -> "Faible"
        2 -> "Modéré"
        3 -> "Fort"
        else -> "Très fort"
    }
}

private fun String.toPollenTypeCode(): String {
    return when (this) {
        "GRASS" -> "GRASS"
        "WEED" -> "WEED"
        else -> "TREE"
    }
}

private fun String.toPollenTypeDisplayName(): String {
    return when (this) {
        "GRASS" -> "Graminées"
        "WEED" -> "Herbacées"
        else -> "Arbres"
    }
}

private fun String.normalizePollenLabel(): String {
    val normalized = Normalizer.normalize(this, Normalizer.Form.NFD)
    return normalized
        .replace("\\p{Mn}+".toRegex(), "")
        .lowercase(Locale.ROOT)
}
