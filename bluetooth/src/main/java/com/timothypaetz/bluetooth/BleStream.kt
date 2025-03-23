package com.timothypaetz.bluetooth

data class BleStream(
    val headerBytes: ByteArray,
    val streamBytes: ByteArray
) {
    /**
     * Generated equals code - can be regenerated if constructor changes
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BleStream

        if (!headerBytes.contentEquals(other.headerBytes)) return false
        if (!streamBytes.contentEquals(other.streamBytes)) return false

        return true
    }

    /**
     * Generated hashCode - can be regenerated if constructor changes
     */
    override fun hashCode(): Int {
        var result = headerBytes.contentHashCode()
        result = 31 * result + streamBytes.contentHashCode()
        return result
    }
}