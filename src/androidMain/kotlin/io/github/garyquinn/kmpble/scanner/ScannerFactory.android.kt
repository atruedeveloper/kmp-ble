package io.github.garyquinn.kmpble.scanner

import io.github.garyquinn.kmpble.KmpBle

public actual fun Scanner(configure: ScannerConfig.() -> Unit): Scanner =
    AndroidScanner(KmpBle.requireContext(), configure)
