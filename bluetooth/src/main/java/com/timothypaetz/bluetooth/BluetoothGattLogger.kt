package com.timothypaetz.bluetooth

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Decorator for BluetoothGattCallback to log all callbacks.  Exceptions thrown in the
 * decorated callback are also logged.
 *
 * @param bleLogger BleLogger implemented by clients
 * @param decoratedCallback BluetoothGattCallback which is wrapped
 */
internal class BluetoothGattLogger(
    private val bleLogger: BleLogger,
    private val decoratedCallback: BluetoothGattCallback
) : BluetoothGattCallback() {
    override fun onPhyUpdate(gatt: BluetoothGatt, txPhy: Int, rxPhy: Int, status: Int) {
        try {
            log("onPhyUpdate txPhy: $txPhy rxPhy: $rxPhy status: ${status.label()}")
            decoratedCallback.onPhyUpdate(gatt, txPhy, rxPhy, status)
        } catch (e: Exception) {
            logException(e)
        }
    }

    override fun onPhyRead(gatt: BluetoothGatt, txPhy: Int, rxPhy: Int, status: Int) {
        try {
            log("onPhyRead txPhy: $txPhy rxPhy: $rxPhy status: ${status.label()}")
            decoratedCallback.onPhyRead(gatt, txPhy, rxPhy, status)
        } catch (e: Exception) {
            logException(e)
        }
    }

    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        try {
            super.onConnectionStateChange(gatt, status, newState)
            log("onConnectionStateChange ${newState.stateLabel()} status: ${status.label()}")
            decoratedCallback.onConnectionStateChange(gatt, status, newState)
        } catch (e: Exception) {
            logException(e)
        }

    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        try {
            log("onServicesDiscovered status: ${status.label()}")
            decoratedCallback.onServicesDiscovered(gatt, status)
        } catch (e: Exception) {
            logException(e)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int
    ) {
        try {
            log("onCharacteristicRead status: ${status.label()} characteristic: ${characteristic.uuid} - ${characteristic.value.toHexWithLength()}")
            decoratedCallback.onCharacteristicRead(gatt, characteristic, status)
        } catch (e: Exception) {
            logException(e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        status: Int
    ) {
        try {
            log("onCharacteristicRead status: ${status.label()} characteristic: ${characteristic.uuid} - ${characteristic.value.toHexWithLength()}")
            decoratedCallback.onCharacteristicRead(gatt, characteristic, value, status)
        } catch (e: Exception) {
            logException(e)
        }
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int
    ) {
        try {
            log("onCharacteristicWrite status: ${status.label()} characteristic: ${characteristic.uuid} - ${characteristic.value.toHexWithLength()}")
            decoratedCallback.onCharacteristicWrite(gatt, characteristic, status)
        } catch (e: Exception) {
            logException(e)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ) {
        try {
            log("onCharacteristicChanged characteristic: ${characteristic.uuid} - ${characteristic.value.toHexWithLength()}")
            decoratedCallback.onCharacteristicChanged(gatt, characteristic)
        } catch (e: Exception) {
            logException(e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        try {
            log("onCharacteristicChanged characteristic: ${characteristic.uuid} - ${value.toHexWithLength()}")
            decoratedCallback.onCharacteristicChanged(gatt, characteristic, value)
        } catch (e: Exception) {
            logException(e)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onDescriptorRead(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        status: Int
    ) {
        try {
            log("onDescriptorRead status: ${status.label()} descriptor ${descriptor.uuid} data: ${descriptor.value?.toHexWithLength()}")
            decoratedCallback.onDescriptorRead(gatt, descriptor, status)
        } catch (e: Exception) {
            logException(e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onDescriptorRead(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        status: Int,
        value: ByteArray
    ) {
        try {
            log("onDescriptorRead status: ${status.label()} descriptor ${descriptor.uuid} data: ${value.toHexWithLength()}")
            decoratedCallback.onDescriptorRead(gatt, descriptor, status, value)
        } catch (e: Exception) {
            logException(e)
        }
    }

    override fun onDescriptorWrite(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        status: Int
    ) {
        try {
            log("onDescriptorWrite status: ${status.label()} descriptor ${descriptor.uuid} data: ${descriptor.value?.toHexWithLength()}")
            decoratedCallback.onDescriptorWrite(gatt, descriptor, status)
        } catch (e: Exception) {
            logException(e)
        }
    }

    override fun onReliableWriteCompleted(gatt: BluetoothGatt, status: Int) {
        try {
            log("onReliableWriteCompleted status: ${status.label()}")
            decoratedCallback.onReliableWriteCompleted(gatt, status)
        } catch (e: Exception) {
            logException(e)
        }
    }

    override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
        try {
            log("onReadRemoteRssi status: ${status.label()} rssi: $rssi")
            decoratedCallback.onReadRemoteRssi(gatt, rssi, status)
        } catch (e: Exception) {
            logException(e)
        }
    }

    override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
        try {
            log("onMtuChanged status: ${status.label()} mtu: $mtu")
            decoratedCallback.onMtuChanged(gatt, mtu, status)
        } catch (e: Exception) {
            logException(e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onServiceChanged(gatt: BluetoothGatt) {
        try {
            log("onServiceChanged")
            decoratedCallback.onServiceChanged(gatt)
        } catch (e: Exception) {
            logException(e)
        }
    }

    private fun log(message: String) {
        bleLogger.log(BleLoggerType.VERBOSE, message)
    }

    private fun logException(e: Exception) {
        bleLogger.log(BleLoggerType.ERROR, "BluetoothGattLogger exception", e)
    }

    private fun Int?.label(): String {
        return if (this == BluetoothGatt.GATT_SUCCESS) {
            "Success"
        } else {
            log("Failed status: $this")
            "Failed"
        }
    }

    private fun Int?.stateLabel(): String {
        return when (this) {
            BluetoothProfile.STATE_CONNECTED -> "Connected"
            BluetoothProfile.STATE_DISCONNECTED -> "Disconnected"
            BluetoothProfile.STATE_DISCONNECTING -> "Disconnecting"
            BluetoothProfile.STATE_CONNECTING -> "Disconnecting"
            else -> "Unknown State: $this"
        }
    }
}