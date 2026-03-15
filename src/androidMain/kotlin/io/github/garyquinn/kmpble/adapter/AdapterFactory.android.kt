package io.github.garyquinn.kmpble.adapter

import io.github.garyquinn.kmpble.KmpBle

public actual fun BluetoothAdapter(): BluetoothAdapter =
    AndroidBluetoothAdapter(KmpBle.requireContext())
