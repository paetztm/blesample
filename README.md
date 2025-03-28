# Bluetooth Low Energy Sample Code

This is a work in progress example of how to connect to BLE devices.   
This code is not production ready.  
This converts the asynchronous IBinder Thread BluetoothGattCallback callbacks to be blocking calls for the client.  
This code currently uses Threads instead of coroutines.

An example for how to use the Bluetooth library can be found in BleDevice.kt