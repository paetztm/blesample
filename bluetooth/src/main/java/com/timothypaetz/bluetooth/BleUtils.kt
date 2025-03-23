package com.timothypaetz.bluetooth

// https://www.baeldung.com/kotlin/byte-arrays-to-hex-strings
fun ByteArray.toHex(): String = joinToString("") {
    java.lang.Byte.toUnsignedInt(it).toString(radix = 16).padStart(2, '0')
}

fun ByteArray.toHexWithLength(): String = "[${this.size}]${toHex()}"

// https://stackoverflow.com/a/66614516/1236327
fun String.decodeHex(): ByteArray {
    check(length % 2 == 0) { "Must have an even length" }
    return chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
}