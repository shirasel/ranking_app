package com.ytranklab.collection.reporting

interface CollectionReporter {
    fun skippedSource(sourceName: String, message: String)
}

class SystemCollectionReporter : CollectionReporter {
    override fun skippedSource(sourceName: String, message: String) {
        System.err.println("Skipped YouTube source '$sourceName': $message")
    }
}
