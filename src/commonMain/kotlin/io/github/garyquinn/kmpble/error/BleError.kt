package io.github.garyquinn.kmpble.error

public sealed class BleError {
    public data class ConnectionFailed(val reason: String, val platformCode: Int? = null) : BleError()
    public data class ConnectionLost(val reason: String, val platformCode: Int? = null) : BleError()
    public data class GattError(val operation: String, val status: GattStatus) : BleError()
    public data class OperationFailed(val message: String) : BleError()
}
