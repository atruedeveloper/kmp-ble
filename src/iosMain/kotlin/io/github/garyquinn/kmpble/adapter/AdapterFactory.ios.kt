package io.github.garyquinn.kmpble.adapter

public actual fun BluetoothAdapter(): BluetoothAdapter =
    IosBluetoothAdapter()
