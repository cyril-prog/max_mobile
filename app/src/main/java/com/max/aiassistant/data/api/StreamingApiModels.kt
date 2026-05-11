package com.max.aiassistant.data.api

data class StreamingRelease(
    val id: String,
    val title: String,
    val overview: String,
    val showType: String,
    val platformId: String,
    val platformName: String,
    val addedAt: String,
    val releaseYear: Int?,
    val rating: Int?,
    val posterUrl: String,
    val link: String
)
