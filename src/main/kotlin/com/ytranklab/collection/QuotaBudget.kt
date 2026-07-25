package com.ytranklab.collection

class QuotaBudget(
    private val maxEstimatedQuotaUnits: Int,
    private val reservedDetailQuotaUnits: Int,
) {
    private var spent = 0

    fun trySpend(cost: Int): Boolean {
        if (maxEstimatedQuotaUnits <= 0) {
            spent += cost
            return true
        }
        if (spent + cost + reservedDetailQuotaUnits > maxEstimatedQuotaUnits) return false
        spent += cost
        return true
    }
}
