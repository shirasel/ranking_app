package com.ytranklab.collection

class CollectionQuotaEstimator {
    fun estimate(
        sourceResults: List<SourceCollectionResult>,
        fetchedVideoIds: Int,
        uniqueChannels: Int,
    ): Int {
        val searchUnits = sourceResults.count { it.source.startsWith("keyword:") && it.status == "ok" } * 100
        val popularUnits = sourceResults.count { it.source.startsWith("popular:") && it.status == "ok" }
        val channelUnits = sourceResults.count { it.source.startsWith("channel:") && it.status == "ok" }
        val playlistUnits = channelUnits
        val videoUnits = if (fetchedVideoIds > 0) ((fetchedVideoIds - 1) / 50) + 1 else 0
        val subscriberChannelUnits = if (uniqueChannels > 0) ((uniqueChannels - 1) / 50) + 1 else 0
        return searchUnits + popularUnits + channelUnits + playlistUnits + videoUnits + subscriberChannelUnits
    }
}
