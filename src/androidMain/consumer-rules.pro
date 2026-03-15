# kmp-ble consumer ProGuard rules
# Keep BLE callback classes (Android GATT callbacks use reflection)
-keep class io.github.garyquinn.kmpble.peripheral.AndroidGattBridge$* { *; }
