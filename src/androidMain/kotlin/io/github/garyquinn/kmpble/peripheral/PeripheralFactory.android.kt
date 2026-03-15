package io.github.garyquinn.kmpble.peripheral

import android.bluetooth.le.ScanResult
import io.github.garyquinn.kmpble.KmpBle
import io.github.garyquinn.kmpble.peripheral.internal.PeripheralRegistry
import io.github.garyquinn.kmpble.scanner.Advertisement

public actual fun Advertisement.toPeripheral(): Peripheral {
    val scanResult = platformContext as? ScanResult
        ?: throw IllegalStateException(
            "Cannot create Peripheral: Advertisement was not produced by AndroidScanner"
        )
    return PeripheralRegistry.getOrCreate(identifier) {
        AndroidPeripheral(scanResult.device, KmpBle.requireContext())
    }
}
