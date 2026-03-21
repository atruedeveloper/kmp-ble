package com.atruedev.kmpble.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.atruedev.kmpble.BleData
import com.atruedev.kmpble.ExperimentalBleApi
import com.atruedev.kmpble.connection.Phy
import com.atruedev.kmpble.scanner.uuidFrom
import com.atruedev.kmpble.server.AdvertiseConfig
import com.atruedev.kmpble.server.AdvertiseInterval
import com.atruedev.kmpble.server.Advertiser
import com.atruedev.kmpble.server.ExtendedAdvertiseConfig
import com.atruedev.kmpble.server.ExtendedAdvertiser
import com.atruedev.kmpble.server.GattServer
import com.atruedev.kmpble.server.ServerConnectionEvent
import kotlinx.coroutines.launch

private val SAMPLE_SERVICE_UUID = uuidFrom("180D") // Heart Rate
private val SAMPLE_CHAR_UUID = uuidFrom("2A37")    // Heart Rate Measurement

@OptIn(ExperimentalMaterial3Api::class, ExperimentalBleApi::class, ExperimentalLayoutApi::class)
@Composable
fun ServerScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    // GATT Server
    var serverOpen by remember { mutableStateOf(false) }
    var heartRate by remember { mutableStateOf(72) }
    val server = remember {
        GattServer {
            service(SAMPLE_SERVICE_UUID) {
                characteristic(SAMPLE_CHAR_UUID) {
                    properties {
                        read = true
                        notify = true
                    }
                    permissions { read = true }
                    onRead { _ -> BleData(byteArrayOf(0x00, heartRate.toByte())) }
                }
            }
        }
    }

    // Legacy Advertiser
    val advertiser = remember { Advertiser() }
    val isAdvertising by advertiser.isAdvertising.collectAsState()

    // Extended Advertiser (BLE 5.0)
    val extAdvertiser = remember { ExtendedAdvertiser() }
    val activeSets by extAdvertiser.activeSets.collectAsState()

    // Connection events log
    var connectionLog by remember { mutableStateOf(listOf<String>()) }

    // Track server connection events
    if (serverOpen) {
        DisposableEffect(Unit) {
            val job = scope.launch {
                server.connectionEvents.collect { event ->
                    val msg = when (event) {
                        is ServerConnectionEvent.Connected -> "Connected: ${event.device.value.take(8)}"
                        is ServerConnectionEvent.Disconnected -> "Disconnected: ${event.device.value.take(8)}"
                    }
                    connectionLog = (listOf(msg) + connectionLog).take(20)
                }
            }
            onDispose { job.cancel() }
        }
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            advertiser.close()
            extAdvertiser.close()
            server.close()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GATT Server & Advertiser") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("<", style = MaterialTheme.typography.titleLarge)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // GATT Server section
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("GATT Server", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))

                        Text(
                            "Hosts a Heart Rate service (0x180D) with a readable/notifiable measurement characteristic.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Spacer(Modifier.height(8.dp))

                        FilterChip(
                            selected = serverOpen,
                            onClick = {},
                            label = { Text(if (serverOpen) "Open" else "Closed") },
                        )

                        Spacer(Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        try {
                                            server.open()
                                            serverOpen = true
                                        } catch (e: Exception) {
                                            snackbar.showSnackbar("Open failed: ${e.message}")
                                        }
                                    }
                                },
                                enabled = !serverOpen,
                            ) { Text("Open") }

                            OutlinedButton(
                                onClick = {
                                    server.close()
                                    serverOpen = false
                                },
                                enabled = serverOpen,
                            ) { Text("Close") }
                        }

                        if (serverOpen) {
                            Spacer(Modifier.height(8.dp))

                            // Heart rate simulator
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text("HR: $heartRate bpm", style = MaterialTheme.typography.bodySmall)
                                OutlinedButton(onClick = { heartRate = (60..180).random() }) {
                                    Text("Randomize")
                                }
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                server.notify(
                                                    SAMPLE_CHAR_UUID,
                                                    null,
                                                    BleData(byteArrayOf(0x00, heartRate.toByte())),
                                                )
                                            } catch (e: Exception) {
                                                snackbar.showSnackbar("Notify failed: ${e.message}")
                                            }
                                        }
                                    },
                                ) { Text("Notify All") }
                            }
                        }
                    }
                }
            }

            // Legacy Advertiser section
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Legacy Advertiser", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))

                        FilterChip(
                            selected = isAdvertising,
                            onClick = {},
                            label = { Text(if (isAdvertising) "Advertising" else "Stopped") },
                        )

                        Spacer(Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    try {
                                        advertiser.startAdvertising(
                                            AdvertiseConfig(
                                                name = "kmp-ble Sample",
                                                serviceUuids = listOf(SAMPLE_SERVICE_UUID),
                                                connectable = true,
                                            ),
                                        )
                                    } catch (e: Exception) {
                                        scope.launch { snackbar.showSnackbar("Start failed: ${e.message}") }
                                    }
                                },
                                enabled = !isAdvertising,
                            ) { Text("Start") }

                            OutlinedButton(
                                onClick = { advertiser.stopAdvertising() },
                                enabled = isAdvertising,
                            ) { Text("Stop") }
                        }
                    }
                }
            }

            // Extended Advertiser section (BLE 5.0)
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Extended Advertiser (BLE 5.0)", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))

                        Text(
                            "Supports larger payloads, PHY selection, and multiple concurrent advertising sets.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "Active sets: ${activeSets.size}",
                            style = MaterialTheme.typography.bodySmall,
                        )

                        Spacer(Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        try {
                                            extAdvertiser.startAdvertisingSet(
                                                ExtendedAdvertiseConfig(
                                                    name = "kmp-ble Ext",
                                                    serviceUuids = listOf(SAMPLE_SERVICE_UUID),
                                                    connectable = true,
                                                    primaryPhy = Phy.Le1M,
                                                    secondaryPhy = Phy.Le2M,
                                                    interval = AdvertiseInterval.Balanced,
                                                ),
                                            )
                                        } catch (e: Exception) {
                                            snackbar.showSnackbar("Start failed: ${e.message}")
                                        }
                                    }
                                },
                            ) { Text("Add Set (1M/2M)") }

                            Button(
                                onClick = {
                                    scope.launch {
                                        try {
                                            extAdvertiser.startAdvertisingSet(
                                                ExtendedAdvertiseConfig(
                                                    name = "kmp-ble LR",
                                                    serviceUuids = listOf(SAMPLE_SERVICE_UUID),
                                                    connectable = true,
                                                    primaryPhy = Phy.LeCoded,
                                                    secondaryPhy = Phy.LeCoded,
                                                    interval = AdvertiseInterval.LowPower,
                                                ),
                                            )
                                        } catch (e: Exception) {
                                            snackbar.showSnackbar("Start failed: ${e.message}")
                                        }
                                    }
                                },
                            ) { Text("Add Set (Coded)") }
                        }

                        if (activeSets.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                for (setId in activeSets) {
                                    FilterChip(
                                        selected = true,
                                        onClick = {
                                            scope.launch { extAdvertiser.stopAdvertisingSet(setId) }
                                        },
                                        label = { Text("Set $setId (tap to stop)") },
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { extAdvertiser.close() },
                            enabled = activeSets.isNotEmpty(),
                        ) { Text("Stop All") }
                    }
                }
            }

            // Connection log
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Connection Events", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))

                        if (connectionLog.isEmpty()) {
                            Text(
                                "No connection events yet. Open the server and connect a client.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            for (entry in connectionLog) {
                                Text(entry, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
