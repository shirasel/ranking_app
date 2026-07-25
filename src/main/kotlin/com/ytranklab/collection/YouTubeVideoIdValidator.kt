package com.ytranklab.collection

class YouTubeVideoIdValidator {
    fun isValid(videoId: String): Boolean =
        videoId.matches(Regex("^[A-Za-z0-9_-]{6,64}$"))
}
