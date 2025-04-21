package com.example.ble

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.Modifier
import com.example.ble.ui.theme.BLETheme

class MainActivity : ComponentActivity() {
    private val LOCATION_PERMISSION = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Pedimos permiso de ubicación si no está concedido
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED)
        {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION
            )
        }

        setContent {
            BLETheme {
                BLEScanScreen()
            }
        }
    }

    // Manejo simple de la respuesta de permiso
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION &&
            grantResults.firstOrNull() != PackageManager.PERMISSION_GRANTED)
        {
            Toast.makeText(
                this,
                "Necesitamos permiso de ubicación para escanear BLE",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
