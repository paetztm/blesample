package com.timothypaetz.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import java.io.ByteArrayOutputStream
import java.util.Collections
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SuppressLint("MissingPermission")
class BleConnection(
    context: Context,
    val macAddress: String,
    private val bleLogger: BleLogger,
    private val callback: OnBleConnectionCallback
) {

    interface OnBleConnectionCallback {
        fun onConnected(bleConnection: BleConnection)
        fun onDisconnected(bleConnection: BleConnection)
    }

    private val executor = Executors.newSingleThreadScheduledExecutor()
    private val applicationContext = context.applicationContext
    private val lock = Object()

    private var bluetoothGatt: BluetoothGatt? = null
    private var closeGatt = Runnable {
        this.closeGatt()
    }
    private var bluetoothDevice: BluetoothDevice? = null
    private val bondLatch = CountDownLatch(1)
    private val discoverServiceLatch = CountDownLatch(1)
    private val characteristicsMap =
        Collections.synchronizedMap(mutableMapOf<UUID, BluetoothGattCharacteristic>())

    private var descriptorWriteLatch: CountDownLatch = CountDownLatch(1)
    private val characteristicLatches = Collections.synchronizedMap(
        mutableMapOf<UUID, CountDownLatch>()
    )
    private val characteristicStreams = Collections.synchronizedMap(
        mutableMapOf<UUID, ByteArrayOutputStream>()
    )

    private val descriptorUUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    fun connect() {
        bleLogger.log(BleLoggerType.VERBOSE, "Connecting")
        verifyPreconnectedState()
        val adapter = applicationContext.bluetoothAdapter() ?: run {
            return
        }

        executor.execute {
            synchronized(lock) {
                bluetoothDevice = adapter.getRemoteDevice(macAddress)
                bluetoothGatt = bluetoothDevice?.connectGatt(
                    applicationContext,
                    true,
                    BluetoothGattLogger(bleLogger, gattCallback)
                )
            }
        }

    }

    fun disconnect() {
        bleLogger.log(BleLoggerType.VERBOSE, "Disconnecting")
        synchronized(lock) {
            bluetoothGatt?.disconnect()
        }
        executor.schedule(closeGatt, 3, TimeUnit.SECONDS)
    }

    private fun closeGatt() {
        synchronized(lock) {
            bluetoothGatt?.close()
            bluetoothGatt = null
        }
    }

    fun isBonded(): Boolean {
        synchronized(lock) {
            return bluetoothDevice?.bondState == BluetoothDevice.BOND_BONDED
        }
    }

    fun bond(timeoutInSeconds: Long = 25): Boolean {
        bleLogger.log(BleLoggerType.VERBOSE, "Bonding")
        if (isBonded()) {
            bleLogger.log(BleLoggerType.WARNING, "Trying to bond when already bonded")
            return true
        }
        registerBondListener()
        synchronized(lock) {
            bluetoothDevice?.createBond()
        }
        val result = bondLatch.await(timeoutInSeconds, TimeUnit.SECONDS)
        unregisterBondListener()
        return result
    }

    fun discoverServices(timeoutInSeconds: Long = 3): Boolean {
        bleLogger.log(BleLoggerType.VERBOSE, "Discovering Services")
        val gatt = getGatt()

        executor.execute {
            gatt.discoverServices()
        }

        if (!discoverServiceLatch.await(timeoutInSeconds, TimeUnit.SECONDS)) {
            bleLogger.log(BleLoggerType.WARNING, "Failed to discover services")
            return false
        }

        return true
    }

    fun enableIndications(bleCharacteristic: BleCharacteristic): Boolean {
        bleLogger.log(BleLoggerType.VERBOSE, "Enabling Indications: ${bleCharacteristic.name}")
        return executeDescriptor(
            bleCharacteristic,
            true,
            BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        )
    }

    fun enableNotifications(bleCharacteristic: BleCharacteristic): Boolean {
        bleLogger.log(BleLoggerType.VERBOSE, "Enabling Notifications: ${bleCharacteristic.name}")
        return executeDescriptor(
            bleCharacteristic,
            true,
            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        )
    }

    fun disableNotifications(bleCharacteristic: BleCharacteristic): Boolean {
        bleLogger.log(BleLoggerType.VERBOSE, "Disabling Notifications: ${bleCharacteristic.name}")
        return executeDescriptor(
            bleCharacteristic,
            false,
            BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        )
    }

    fun read(bleCharacteristic: BleCharacteristic): ByteArray {
        bleLogger.log(
            BleLoggerType.VERBOSE,
            "Reading from: ${bleCharacteristic.name} - ${bleCharacteristic.uuid}"
        )
        val gattChar = getGattChar(bleCharacteristic.uuid)
        val bleGatt = getGatt()
        val characteristicStream = ByteArrayOutputStream()
        characteristicStreams[bleCharacteristic.uuid] = characteristicStream

        val latch = CountDownLatch(1)
        characteristicLatches[bleCharacteristic.uuid] = latch
        executor.execute {
            val success = bleGatt.readCharacteristic(gattChar)
            if (!success) {
                bleLogger.log(
                    BleLoggerType.WARNING,
                    "Failed to read from characteristic: ${bleCharacteristic.name}"
                )
            }
        }

        latch.await(
            bleCharacteristic.timeoutInMillis,
            TimeUnit.MILLISECONDS
        )

        return characteristicStream.toByteArray()
    }

    fun write(bleCharacteristic: BleCharacteristic, bytes: ByteArray): Boolean {
        bleLogger.log(
            BleLoggerType.VERBOSE,
            "Writing bytes ${bytes.toHexWithLength()} to ${bleCharacteristic.name} - ${bleCharacteristic.uuid}"
        )
        val gattChar = getGattChar(bleCharacteristic.uuid)
        val bleGatt = getGatt()

        val latch = CountDownLatch(1)
        characteristicLatches[bleCharacteristic.uuid] = latch
        executor.execute {
            gattChar.value = bytes
            val isSuccessful = bleGatt.writeCharacteristic(gattChar)

            if (!isSuccessful) {
                bleLogger.log(
                    BleLoggerType.WARNING,
                    "Failed to write characteristic: ${bleCharacteristic.name}"
                )
            }
        }

        return latch.await(
            bleCharacteristic.timeoutInMillis,
            TimeUnit.MILLISECONDS
        )

    }

    fun writeWithAck(bleCharacteristic: BleCharacteristic, bytes: ByteArray): ByteArray {
        bleLogger.log(
            BleLoggerType.VERBOSE,
            "WritingWithAck bytes ${bytes.toHexWithLength()} to ${bleCharacteristic.name} - ${bleCharacteristic.uuid}"
        )
        val gattChar = getGattChar(bleCharacteristic.uuid)
        val bleGatt = getGatt()

        val characteristicStream = ByteArrayOutputStream()
        characteristicStreams[bleCharacteristic.uuid] = characteristicStream

        val latch = CountDownLatch(2)
        characteristicLatches[bleCharacteristic.uuid] = latch
        gattChar.value = bytes
        executor.execute {
            val isSuccessful = bleGatt.writeCharacteristic(gattChar)

            if (!isSuccessful) {
                bleLogger.log(
                    BleLoggerType.WARNING,
                    "Failed to writeWithAck characteristic: ${bleCharacteristic.name}"
                )
            }
        }

        latch.await(
            bleCharacteristic.timeoutInMillis,
            TimeUnit.MILLISECONDS
        )

        return characteristicStream.toByteArray()
    }

    fun writeWithoutResponse(bleCharacteristic: BleCharacteristic, bytes: ByteArray): Boolean {
        bleLogger.log(
            BleLoggerType.VERBOSE,
            "WritingWithoutResponse bytes ${bytes.toHexWithLength()} to ${bleCharacteristic.name} - ${bleCharacteristic.uuid}"
        )
        val gattChar = getGattChar(bleCharacteristic.uuid)
        val bleGatt = getGatt()

        gattChar.value = bytes

        val isSuccessful = bleGatt.writeCharacteristic(gattChar)

        if (!isSuccessful) {
            bleLogger.log(
                BleLoggerType.WARNING,
                "Failed to writeWithoutResponse characteristic: ${bleCharacteristic.name}"
            )
            return false
        }
        return true
    }

    fun stream(
        streamCharacteristic: BleCharacteristic,
        ackCharacteristic: BleCharacteristic,
        bytes: ByteArray
    ): BleStream {
        bleLogger.log(
            BleLoggerType.VERBOSE,
            "Stream bytes ${bytes.toHexWithLength()} to ${streamCharacteristic.name} - ${streamCharacteristic.uuid} with ack ${ackCharacteristic.name} - ${ackCharacteristic.uuid}"
        )
        val characteristicStream = ByteArrayOutputStream()
        characteristicStreams[streamCharacteristic.uuid] = characteristicStream

        return BleStream(
            writeWithAck(ackCharacteristic, bytes),
            characteristicStream.toByteArray()
        )
    }

    private fun executeDescriptor(
        bleCharacteristic: BleCharacteristic,
        isEnabled: Boolean,
        bytes: ByteArray
    ): Boolean {
        val gattChar = getGattChar(bleCharacteristic.uuid)
        val bleGatt = getGatt()

        descriptorWriteLatch = CountDownLatch(1)
        executor.execute {
            val isSuccess = bleGatt.setCharacteristicNotification(gattChar, isEnabled)

            if (!isSuccess) {
                bleLogger.log(
                    BleLoggerType.WARNING,
                    "Failed to set characteristic notification: ${bleCharacteristic.name} - ${bleCharacteristic.uuid}"
                )
            }

            val descriptor = gattChar.getDescriptor(descriptorUUID)
            descriptor.value = bytes
            bleGatt.writeDescriptor(descriptor)
        }

        return descriptorWriteLatch.await(
            bleCharacteristic.timeoutInMillis,
            TimeUnit.MILLISECONDS
        )
    }

    private fun registerBondListener() {
        applicationContext.registerReceiver(
            bondBroadcastReceiver,
            IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        )
    }

    private fun unregisterBondListener() {
        try {
            applicationContext.unregisterReceiver(bondBroadcastReceiver)
        } catch (e: Exception) {
            bleLogger.log(
                BleLoggerType.ERROR,
                "Unregistering bond receiver failed",
                e
            )
        }
    }

    private val bondBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            with(intent) {
                if (action != BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
                    bleLogger.log(
                        BleLoggerType.INFO,
                        "Unknown action return from bond broadcast receiver: $action"
                    )
                }

                val device = getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                if (device == null || device.address != macAddress) {
                    return
                }

                synchronized(lock) {
                    bluetoothDevice = device
                }
                val previousBondState = getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1)
                val bondState = getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)
                val bondTransition = "${previousBondState.toBondStateDescription()} to " +
                        bondState.toBondStateDescription()
                bleLogger.log(BleLoggerType.VERBOSE, "Bond transition: $bondTransition")
                if (bondState == BluetoothDevice.BOND_BONDED) {
                    bondLatch.countDown()
                }
            }
        }

        private fun Int.toBondStateDescription() = when (this) {
            BluetoothDevice.BOND_BONDED -> "BONDED"
            BluetoothDevice.BOND_BONDING -> "BONDING"
            BluetoothDevice.BOND_NONE -> "NOT BONDED"
            else -> "ERROR: $this"
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                callback.onConnected(this@BleConnection)
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                callback.onDisconnected(this@BleConnection)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            gatt.services.forEach {
                it.characteristics.forEach { gattChar ->
                    characteristicsMap[gattChar.uuid] = gattChar
                    // TODO Maybe we only need to update known characteristics and not all of them?
                    gattChar.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                }
            }
            discoverServiceLatch.countDown()
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                characteristicStreams[characteristic.uuid]?.write(characteristic.value)
                characteristicLatches[characteristic.uuid]?.countDown()
            } else {
                logError("Failed to read characteristic")
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                characteristicLatches[characteristic.uuid]?.countDown()
            } else {
                logError("Failed to write characteristic")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            characteristicStreams[characteristic.uuid]?.write(characteristic.value)
            characteristicLatches[characteristic.uuid]?.countDown()
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                descriptorWriteLatch.countDown()
            } else {
                logError("Failed to write descriptor")
            }
        }
    }

    private fun verifyPreconnectedState() {
        val adapter = applicationContext.bluetoothAdapter() ?: run {
            throw illegalState("Bluetooth Adapter is null")
        }

        if (!BluetoothAdapter.checkBluetoothAddress(macAddress)) {
            throw illegalState("Trying to connect with a bad mac address: $macAddress")
        }

        if (adapter.state != BluetoothAdapter.STATE_ON) {
            throw illegalState("Bluetooth adapter state is not on")
        }
    }

    private fun getGattChar(charUUID: UUID): BluetoothGattCharacteristic {
        return characteristicsMap[charUUID] ?: run {
            throw illegalState("Gatt characteristic null for $charUUID")
        }
    }

    private fun getGatt(): BluetoothGatt {
        synchronized(lock) {
            val bleGatt = bluetoothGatt
            bleGatt ?: run {
                throw illegalState("Bluetooth Gatt is null")
            }
            return bleGatt
        }
    }

    private fun illegalState(message: String): IllegalStateException {
        val e = IllegalStateException(
            message
        )
        bleLogger.log(BleLoggerType.ERROR, message, e)
        return e
    }

    private fun logError(message: String) {
        bleLogger.log(BleLoggerType.ERROR, message)
    }
}