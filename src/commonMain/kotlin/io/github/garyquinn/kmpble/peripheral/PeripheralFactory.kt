package io.github.garyquinn.kmpble.peripheral

import io.github.garyquinn.kmpble.scanner.Advertisement

public expect fun Advertisement.toPeripheral(): Peripheral
