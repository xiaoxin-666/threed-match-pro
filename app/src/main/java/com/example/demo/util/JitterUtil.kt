package com.example.demo.util

import kotlin.math.roundToLong
import kotlin.random.Random

object JitterUtil {
    private const val JITTER_RANGE = 0.2

    fun jitterFactor(): Double {
        val factor = 1.0 + (Random.nextDouble() * 2 - 1) * JITTER_RANGE
        return factor.coerceIn(0.8, 1.2)
    }

    fun applyJitter(baseMs: Long): Long {
        return (baseMs * jitterFactor()).roundToLong()
    }
}
