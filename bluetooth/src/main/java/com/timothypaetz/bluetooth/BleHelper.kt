package com.timothypaetz.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

// TODO: Check if this is right on each version of Android
fun Context.getBlePermissions(): List<String> {
    val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Android 12+
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
        // Android 11
        listOf(
            // Unfortunately Android 11 made this permission very difficult to get.  You have to launch settings and the user has to manually grant this
            // Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
        // Android 10
        listOf(
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    } else {
        listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    }
    return perms
}

fun Context.hasBlePermissions(): Boolean {
    return hasPermissions(getBlePermissions())
}

fun Context.verifyBleWorking() {
    if (!hasBlePermissions()) {
        throw BlePermissionException("BLE Permissions have not been granted")
    } else if (!hasBle()) {
        throw BleDisabledException("BLE does not appear to exist or be enabled")
    }
}

fun Context.hasBle(): Boolean {
    return packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
}

fun Context.bluetoothManager(): BluetoothManager? {
    return getSystemService(BluetoothManager::class.java)
}

fun Context.bluetoothAdapter(): BluetoothAdapter? {
    return bluetoothManager()?.adapter
}

fun Context.isBleEnabled(): Boolean {
    return hasBle() && bluetoothAdapter()?.isEnabled == true
}

fun Context.hasPermissions(permissions: List<String>): Boolean {
    return permissions.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }
}
