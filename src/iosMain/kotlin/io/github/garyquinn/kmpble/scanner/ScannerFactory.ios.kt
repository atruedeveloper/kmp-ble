package io.github.garyquinn.kmpble.scanner

public actual fun Scanner(configure: ScannerConfig.() -> Unit): Scanner =
    IosScanner(configure)
