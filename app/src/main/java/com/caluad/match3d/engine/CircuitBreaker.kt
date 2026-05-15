package com.caluad.match3d.engine

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean

enum class CircuitState { CLOSED, OPEN, HALF_OPEN }

class CircuitBreaker(
    private val failureThreshold: Int = 5,
    private val windowMs: Long = 30_000,
    private val cooldownMs: Long = 30_000
) {
    private val _state = MutableStateFlow(CircuitState.CLOSED)
    val state: StateFlow<CircuitState> = _state

    private val lock = Mutex()
    private val probeInFlight = AtomicBoolean(false)
    private var failureCount = 0
    private var windowStart = 0L
    private var openSince = 0L

    suspend fun recordSuccess() {
        lock.withLock {
            failureCount = 0
            probeInFlight.set(false)
            if (_state.value == CircuitState.HALF_OPEN) {
                _state.value = CircuitState.CLOSED
            }
        }
    }

    suspend fun recordFailure() {
        lock.withLock {
            val now = System.currentTimeMillis()
            if (now - windowStart > windowMs) {
                windowStart = now
                failureCount = 0
            }
            failureCount++
            probeInFlight.set(false)
            if (failureCount >= failureThreshold && _state.value == CircuitState.CLOSED) {
                _state.value = CircuitState.OPEN
                openSince = now
            } else if (_state.value == CircuitState.HALF_OPEN) {
                _state.value = CircuitState.OPEN
                openSince = now
            }
        }
    }

    suspend fun waitIfOpen() {
        while (_state.value != CircuitState.CLOSED) {
            when (_state.value) {
                CircuitState.HALF_OPEN -> {
                    if (probeInFlight.compareAndSet(false, true)) {
                        return
                    }
                    delay(100)
                }
                CircuitState.OPEN -> {
                    val elapsed = System.currentTimeMillis() - openSince
                    if (elapsed >= cooldownMs) {
                        _state.value = CircuitState.HALF_OPEN
                    } else {
                        delay(500)
                    }
                }
                else -> {}
            }
        }
    }
}
