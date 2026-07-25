package com.ytranklab.genre

import com.ytranklab.domain.GenreScore
import com.ytranklab.domain.YouTubeVideo

interface GenreClassifier {
    fun classify(video: YouTubeVideo): List<GenreScore>
}
