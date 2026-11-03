package com.personal.ytmusicdownloader.model

enum class AudioFormat(
    val extension: String,
    val mimeType: String,
    val ytdlFormat: String
) {
    MP3("mp3", "audio/mpeg", "mp3"),
    M4A("m4a", "audio/mp4", "m4a"),
    OPUS("opus", "audio/opus", "opus"),
    FLAC("flac", "audio/flac", "flac")
}

