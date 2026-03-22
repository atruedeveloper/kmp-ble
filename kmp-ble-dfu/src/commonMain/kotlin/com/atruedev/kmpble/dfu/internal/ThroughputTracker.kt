package com.atruedev.kmpble.dfu.internal

import kotlin.time.TimeSource

internal class ThroughputTracker(
    private val windowSize: Int = 10,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    private val samples = ArrayDeque<Sample>(windowSize)
    private val startMark = timeSource.markNow()

    private data class Sample(val timestamp: Long, val bytes: Long)

    fun record(bytesSent: Long) {
        val elapsed = startMark.elapsedNow().inWholeMilliseconds
        samples.addLast(Sample(elapsed, bytesSent))
        if (samples.size > windowSize) samples.removeFirst()
    }

    fun bytesPerSecond(): Long {
        if (samples.size < 2) return 0
        val oldest = samples.first()
        val newest = samples.last()
        val durationMs = newest.timestamp - oldest.timestamp
        if (durationMs <= 0) return 0
        val deltaBytes = newest.bytes - oldest.bytes
        return (deltaBytes * 1000) / durationMs
    }
}
