package com.timothypaetz.bluetooth

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import androidx.annotation.RequiresPermission
import java.util.Collections

/**
 * BLE scanner to find devices.  @param onBleScannerCallback returns found devices or failure reason
 *
 * @param context Context for the application
 * @param bleLogger BleLogger for logging information
 * @param onBleScannerCallback Callback for when BLE device is found or failure reason
 */
class BleScanner(
    private val context: Context,
    private val bleLogger: BleLogger,
    private val onBleScannerCallback: OnBleScannerCallback,
) {

    interface OnBleScannerCallback {
        fun onFound(bleScanner: BleScanner, bluetoothDevice: BluetoothDevice)
        fun onFailure(bleScanner: BleScanner, failReason: Int)
    }

    private val devices: MutableList<String> = Collections.synchronizedList(ArrayList())

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            if (result == null) return
            // This doesn't work :( https://stackoverflow.com/a/51750367/1236327
            // if (callbackType != ScanSettings.CALLBACK_TYPE_FIRST_MATCH) return

            val address = result.device.address
            synchronized(devices) {
                if (devices.contains(address)) return // already found
                devices.add(address)

                onBleScannerCallback.onFound(this@BleScanner, result.device)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            onBleScannerCallback.onFailure(this@BleScanner, errorCode)
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startScan(scanFilter: ScanFilter, scanSettings: ScanSettings) {
        context.verifyBleWorking()
        synchronized(devices) {
            devices.clear()
        }
        val leScanner = getLeScanner()
        if (leScanner != null) {
            leScanner.startScan(listOf(scanFilter), scanSettings, scanCallback)
        } else {
            bleLogger.log(BleLoggerType.ERROR, "Bluetooth LE Scanner is null")
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScan() {
        getLeScanner()?.stopScan(scanCallback)
    }

    private fun getLeScanner() = context.bluetoothAdapter()?.bluetoothLeScanner
}