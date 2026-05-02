package com.example.chikauto.ui.components

fun calculateDays(startMillis: Long, endMillis: Long): Int {
    if (startMillis <= 0L || endMillis <= 0L) return 0
    if (endMillis < startMillis) return 0

    val oneDay = 24 * 60 * 60 * 1000L
    return (((endMillis - startMillis) / oneDay) + 1).toInt()
}

fun dateRangesOverlap(
    start1: Long,
    end1: Long,
    start2: Long,
    end2: Long
): Boolean {
    return start1 <= end2 && start2 <= end1
}