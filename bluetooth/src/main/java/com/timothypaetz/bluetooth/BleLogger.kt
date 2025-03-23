package com.timothypaetz.bluetooth

/**
 * Logger interface for clients to implement
 */
interface BleLogger {
    /**
     * @param bleLoggerType Verbosity for the log
     * @param message Message to be logged
     * @param exception Optional exception to be logged
     * @param tag Optional logging tag - defaults to 'BleLogger'
     */
    fun log(
        bleLoggerType: BleLoggerType,
        message: String,
        exception: Exception? = null,
        tag: String = "BleLogger"
    )
}

enum class BleLoggerType {
    VERBOSE,
    INFO,
    WARNING,
    ERROR
}
