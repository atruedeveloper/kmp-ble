package com.atruedev.kmpble.dfu

public sealed class DfuError(message: String, cause: Throwable? = null) : Exception(message, cause) {

    public class NotConnected(
        message: String = "Peripheral is not connected",
    ) : DfuError(message)

    public class ServiceNotFound(
        message: String = "DFU service not found on peripheral",
    ) : DfuError(message)

    public class CharacteristicNotFound(
        public val name: String,
        message: String = "DFU characteristic not found: $name",
    ) : DfuError(message)

    public class ProtocolError(
        public val opcode: Int,
        public val resultCode: Int,
        message: String,
    ) : DfuError(message)

    public class ChecksumMismatch(
        public val expected: UInt,
        public val actual: UInt,
    ) : DfuError("CRC32 mismatch: expected 0x${expected.toString(16)}, actual 0x${actual.toString(16)}")

    public class TransferFailed(
        message: String,
        cause: Throwable? = null,
    ) : DfuError(message, cause)

    public class FirmwareParseError(
        message: String,
        cause: Throwable? = null,
    ) : DfuError(message, cause)

    public class Timeout(
        message: String = "DFU operation timed out",
    ) : DfuError(message)

    public class Aborted(
        message: String = "DFU was aborted",
    ) : DfuError(message)
}
