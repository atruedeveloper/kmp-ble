package com.atruedev.kmpble.sample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atruedev.kmpble.ExperimentalBleApi
import com.atruedev.kmpble.benchmark.LatencyTracker
import com.atruedev.kmpble.benchmark.ThroughputMeter
import com.atruedev.kmpble.benchmark.bleStopwatch
import com.atruedev.kmpble.bonding.PairingEvent
import com.atruedev.kmpble.bonding.PairingHandler
import com.atruedev.kmpble.bonding.PairingResponse
import com.atruedev.kmpble.connection.ConnectionOptions
import com.atruedev.kmpble.connection.State
import com.atruedev.kmpble.gatt.BackpressureStrategy
import com.atruedev.kmpble.gatt.Characteristic
import com.atruedev.kmpble.gatt.Observation
import com.atruedev.kmpble.gatt.WriteType
import com.atruedev.kmpble.peripheral.Peripheral
import com.atruedev.kmpble.scanner.Advertisement
import com.atruedev.kmpble.peripheral.toPeripheral
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Lifecycle-scoped peripheral management.
 *
 * This is the recommended pattern for using kmp-ble in a Compose app:
 * - The [Peripheral] is created once from the [Advertisement]
 * - All BLE operations are scoped to [viewModelScope]
 * - [onCleared] calls [Peripheral.close], which cancels coroutines and disconnects
 *
 * Without this pattern, GATT connections leak when the screen is removed from
 * the composition. On Android the OS-level connection persists, causing phantom
 * connections that drain battery and block reconnection.
 */
class BleViewModel(advertisement: Advertisement) : ViewModel() {

    private val peripheral: Peripheral = advertisement.toPeripheral()

    val connectionState: StateFlow<State> = peripheral.state
    val bondState = peripheral.bondState
    val services = peripheral.services
    val maximumWriteValueLength = peripheral.maximumWriteValueLength

    private val _rssi = MutableStateFlow<Int?>(null)
    val rssi: StateFlow<Int?> = _rssi.asStateFlow()

    private val _mtu = MutableStateFlow(23)
    val mtu: StateFlow<Int> = _mtu.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // --- Pairing Handler ---
    // Pending pairing event waiting for user response
    private val _pairingEvent = MutableStateFlow<PairingEvent?>(null)
    val pairingEvent: StateFlow<PairingEvent?> = _pairingEvent.asStateFlow()

    // Continuation for pairing response
    private var pairingContinuation: ((PairingResponse) -> Unit)? = null

    @OptIn(ExperimentalBleApi::class)
    val pairingHandler = PairingHandler { event ->
        _pairingEvent.value = event
        suspendCancellableCoroutine { cont ->
            pairingContinuation = { response ->
                _pairingEvent.value = null
                pairingContinuation = null
                cont.resumeWith(Result.success(response))
            }
        }
    }

    fun respondToPairing(response: PairingResponse) {
        pairingContinuation?.invoke(response)
    }

    // --- Benchmark ---
    private val _benchmarkResult = MutableStateFlow<String?>(null)
    val benchmarkResult: StateFlow<String?> = _benchmarkResult.asStateFlow()

    @OptIn(ExperimentalBleApi::class)
    fun benchmarkConnect(options: ConnectionOptions) {
        viewModelScope.launch {
            try {
                _benchmarkResult.value = "Benchmarking connect..."
                peripheral.disconnect()
                val result = bleStopwatch("connect") { peripheral.connect(options) }
                _benchmarkResult.value = "Connect: ${result.duration}"
            } catch (e: Exception) {
                _benchmarkResult.value = "Error: ${formatError(e)}"
            }
        }
    }

    @OptIn(ExperimentalBleApi::class)
    fun benchmarkReads(characteristic: Characteristic, count: Int = 10) {
        viewModelScope.launch {
            try {
                _benchmarkResult.value = "Reading $count times..."
                val meter = ThroughputMeter()
                val latency = LatencyTracker()
                meter.start()
                repeat(count) {
                    latency.measure {
                        val data = peripheral.read(characteristic)
                        meter.record(data.size)
                    }
                }
                val throughput = meter.stop("reads")
                val stats = latency.summarize("read latency")
                _benchmarkResult.value = "$throughput\n$stats"
            } catch (e: Exception) {
                _benchmarkResult.value = "Error: ${formatError(e)}"
            }
        }
    }

    fun connect(options: ConnectionOptions = ConnectionOptions()) {
        viewModelScope.launch {
            try {
                _error.value = null
                peripheral.connect(options)
            } catch (e: Exception) {
                _error.value = formatError(e)
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            try {
                peripheral.disconnect()
            } catch (e: Exception) {
                _error.value = formatError(e)
            }
        }
    }

    fun readCharacteristic(characteristic: Characteristic, onResult: (Result<ByteArray>) -> Unit) {
        viewModelScope.launch {
            onResult(runCatching { peripheral.read(characteristic) })
        }
    }

    fun writeCharacteristic(
        characteristic: Characteristic,
        data: ByteArray,
        writeType: WriteType = WriteType.WithResponse,
    ) {
        viewModelScope.launch {
            try {
                peripheral.write(characteristic, data, writeType)
            } catch (e: Exception) {
                _error.value = formatError(e)
            }
        }
    }

    fun observe(characteristic: Characteristic): Flow<Observation> =
        peripheral.observe(characteristic, BackpressureStrategy.Latest)

    fun readRssi() {
        viewModelScope.launch {
            try {
                _rssi.value = peripheral.readRssi()
            } catch (e: Exception) {
                _error.value = formatError(e)
            }
        }
    }

    fun requestMtu(mtu: Int) {
        viewModelScope.launch {
            try {
                _mtu.value = peripheral.requestMtu(mtu)
            } catch (e: Exception) {
                _error.value = formatError(e)
            }
        }
    }

    @OptIn(com.atruedev.kmpble.ExperimentalBleApi::class)
    fun removeBond() {
        val result = peripheral.removeBond()
        _error.value = when (result) {
            is com.atruedev.kmpble.bonding.BondRemovalResult.Success -> null
            is com.atruedev.kmpble.bonding.BondRemovalResult.NotSupported -> result.message
            is com.atruedev.kmpble.bonding.BondRemovalResult.Failed -> result.reason
        }
    }

    fun clearError() {
        _error.value = null
    }

    // Called when the composable hosting this ViewModel leaves the composition.
    // On Android: when the Activity/Fragment is destroyed (survives config changes).
    // On iOS: when the compose view is removed.
    //
    // This is critical — without it, the GATT connection leaks.
    override fun onCleared() {
        peripheral.close()
    }

    private fun formatError(e: Exception): String = e.message ?: "Unknown error"
}
