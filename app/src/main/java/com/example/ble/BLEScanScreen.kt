package com.example.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@SuppressLint("MissingPermission")
@Composable
fun BLEScanScreen(
    onDeviceSelected: (BluetoothDevice) -> Unit
) {
    val devices = BLEScanner.devices
    val scanning by BLEScanner.isScanning

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(onClick = { BLEScanner.scanLeDevice() }) {
            Text(if (scanning) "Detener escaneo" else "Iniciar escaneo")
        }
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(devices) { device ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDeviceSelected(device) }
                        .padding(vertical = 8.dp)
                ) {
                    Text(text = "${device.name ?: "Desconocido"} — ${device.address}")
                    Divider()
                }
            }
        }
    }
}
