package com.atruedev.kmpble.profiles.heartrate

public enum class BodySensorLocation {
    Other,
    Chest,
    Wrist,
    Finger,
    Hand,
    EarLobe,
    Foot;

    public companion object {
        private val values = entries.toTypedArray()

        public fun fromByte(value: Int): BodySensorLocation? =
            values.getOrNull(value)
    }
}

public fun parseBodySensorLocation(data: ByteArray): BodySensorLocation? {
    if (data.isEmpty()) return null
    return BodySensorLocation.fromByte(data[0].toInt() and 0xFF)
}
