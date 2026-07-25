package com.ytranklab.collection

interface CollectionReporter {
    fun skippedSource(sourceName: String, message: String)
}

class SystemCollectionReporter : CollectionReporter {
    override fun skippedSource(sourceName: String, message: String) {
        System.err.println("Skipped YouTube source '$sourceName': $message")
    }
}
