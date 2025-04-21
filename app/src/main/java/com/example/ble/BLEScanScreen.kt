package com.example.ble

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue           // <<-- esta línea es clave
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@SuppressLint("MissingPermission")       // suprimes la advertencia de BLUETOOTH_CONNECT
@Composable
fun BLEScanScreen() {
    val devices = BLEScanner.devices
    // Ahora que importaste getValue, este by funcionará sin error
    val isScanning by remember { derivedStateOf { devices.isNotEmpty() } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(onClick = { BLEScanner.scanLeDevice() }) {
            Text(if (isScanning) "Detener escaneo" else "Iniciar escaneo")
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn {
            items(devices) { device ->
                // Aquí ya no marca Lint porque suprimes la verificación
                Text(text = "${device.name ?: "Desconocido"} — ${device.address}")
                Divider()
            }
        }
    }
}
