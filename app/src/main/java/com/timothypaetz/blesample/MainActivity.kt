package com.timothypaetz.blesample

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.timothypaetz.blesample.ui.theme.BLESampleTheme
import com.timothypaetz.bluetooth.getBlePermissions
import com.timothypaetz.bluetooth.hasBlePermissions
import com.timothypaetz.bluetooth.isBleEnabled

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissionMap ->

            if (permissionMap.values.all { it }) {
                showToast("Bluetooth Permissions Granted")
            } else {
                showToast("Permissions not granted")
            }
        }

    private val enableBluetoothLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == Activity.RESULT_OK) {
                showToast("Bluetooth enabled")
            } else {
                showToast("Unable to start bluetooth")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            BLESampleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column {
                        Greeting(
                            name = "Android",
                            modifier = Modifier.padding(innerPadding)
                        )
                        CheckPermissions {
                            if (hasBlePermissions()) {
                                showToast("Already have permissions")
                            } else {
                                requestPermissionLauncher.launch(
                                    getBlePermissions().toTypedArray()
                                )
                            }
                        }
                        EnableBluetooth {
                            if (isBleEnabled()) {
                                showToast("Bluetooth is already enabled")
                            } else {
                                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                enableBluetoothLauncher.launch(enableBtIntent)
                            }
                        }
                        Button(
                            { BleDevice(context).scan() }
                        ) {
                            Text("Start")
                        }
                    }
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }

}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Composable
fun CheckPermissions(onClick: () -> Unit) {
    Button(
        onClick = onClick
    ) {
        Text(text = "Request Permissions")
    }
}

@Composable
fun EnableBluetooth(onClick: () -> Unit) {
    Button(
        onClick = onClick
    ) {
        Text(text = "Enable Bluetooth")
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BLESampleTheme {
        Greeting("Android")
    }
}