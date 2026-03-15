package io.github.garyquinn.kmpble.peripheral

import io.github.garyquinn.kmpble.peripheral.internal.PeripheralRegistry
import io.github.garyquinn.kmpble.scanner.Advertisement
import platform.CoreBluetooth.CBPeripheral

public actual fun Advertisement.toPeripheral(): Peripheral {
    val cbPeripheral = platformContext as? CBPeripheral
        ?: throw IllegalStateException(
            "Cannot create Peripheral: Advertisement was not produced by IosScanner"
        )
    return PeripheralRegistry.getOrCreate(identifier) {
        IosPeripheral(cbPeripheral)
    }
}
