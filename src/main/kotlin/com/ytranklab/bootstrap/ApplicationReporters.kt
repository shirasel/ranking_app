package com.ytranklab.bootstrap

import com.ytranklab.app.reporting.GenerationReporter
import com.ytranklab.collection.reporting.CollectionReporter

data class ApplicationReporters(
    val generationReporter: GenerationReporter,
    val collectionReporter: CollectionReporter,
)
