package com.timothypaetz.blesample

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import androidx.annotation.RequiresPermission
import com.timothypaetz.bluetooth.BleCharacteristic
import com.timothypaetz.bluetooth.BleConnection
import com.timothypaetz.bluetooth.BleLogger
import com.timothypaetz.bluetooth.BleLoggerType
import com.timothypaetz.bluetooth.BleScanner
import java.util.UUID
import java.util.concurrent.Executors

class BleDevice(
    private val context: Context,
) {
    private val ioExecutor = Executors.newSingleThreadExecutor()

    private val logger = object: BleLogger{
        override fun log(
            bleLoggerType: BleLoggerType,
            message: String,
            exception: Exception?,
            tag: String
        ) {
            println("$tag - ${bleLoggerType.name}: $message")
            exception?.printStackTrace()
        }
    }

    private val onBleScannerCallback = object : BleScanner.OnBleScannerCallback {
        @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
        override fun onFound(bleScanner: BleScanner, bluetoothDevice: BluetoothDevice) {
            bleScanner.stopScan()
            onDeviceFound(bluetoothDevice)
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
        override fun onFailure(bleScanner: BleScanner, failReason: Int) {
            bleScanner.stopScan()
            println("Error - scan failed: $failReason")
        }
    }

    private val onBleConnectionCallback = object : BleConnection.OnBleConnectionCallback {
        override fun onConnected(bleConnection: BleConnection) {
            connected(bleConnection)
        }

        override fun onDisconnected(bleConnection: BleConnection) {
            disconnected(bleConnection)
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun scan() {
        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(UUID.randomUUID())) // TODO: SET CORRECT SERVICE UUID
            .setDeviceName("") // TODO: SET CORRECT DEVICE NAME
            .build()
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .build()
        val bleScanner = BleScanner(
            context,
            logger,
            onBleScannerCallback
        )
        bleScanner.startScan(scanFilter, scanSettings)
    }

    private fun onDeviceFound(bluetoothDevice: BluetoothDevice){
        println("Device found: $bluetoothDevice")
        ioExecutor.execute {
            try {
                val connection = BleConnection(
                    context,
                    bluetoothDevice.address,
                    logger,
                    onBleConnectionCallback
                )

                connection.connect()
            } catch (e: Exception) {
                logger.log(
                    BleLoggerType.ERROR,
                    "Exception while trying to connect",
                    e,
                    tag = "BleDevice"
                )
            }
        }
    }

    private fun connected(connection: BleConnection) {
        val discovered = connection.discoverServices()

        if (!discovered){
            println("Error - failed to discover devices")
            return
        }

        // TODO: Define your BLE endpoints
        val characteristic = BleCharacteristic(UUID.randomUUID(), "Random", 3000)
        connection.enableIndications(characteristic)

        connection.read(characteristic)
        connection.write(characteristic, "Random".toByteArray())
        val response = connection.writeWithAck(characteristic, "Random".toByteArray())
    }

    private fun disconnected(connection: BleConnection) {
        // TODO: Implement reconnect logic or something else?
    }
}