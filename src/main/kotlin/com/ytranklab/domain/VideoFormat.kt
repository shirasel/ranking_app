package com.ytranklab.domain

fun YouTubeVideo.isShortVideo(): Boolean {
    durationSeconds?.let { return it <= 60L }
    val text = "$title\n$description".lowercase()
    return text.contains("#shorts") || text.contains("#short") || text.contains("youtube shorts")
}
