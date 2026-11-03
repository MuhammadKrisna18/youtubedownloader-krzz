package com.personal.ytmusicdownloader.model

data class TrackInfo(
    val id: String,
    val title: String,
    val uploader: String,
    val durationSeconds: Long,
    val thumbnailUrl: String,
    val webUrl: String
)

