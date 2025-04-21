package com.example.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ble.ui.theme.BLETheme
@SuppressLint("NewApi")
class MainActivity : ComponentActivity() {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    private var bluetoothService: BluetoothLeService? = null
    private var bound = false

    // 1) ServiceConnection para bind/unbind
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? BluetoothLeService.LocalBinder
            bluetoothService = localBinder?.getService()
            bound = true

            bluetoothService?.let { service ->
                if (!service.initialize()) {
                    Toast.makeText(this@MainActivity, "BLE no soportado", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bluetoothService = null
            bound = false
        }
    }

    // 2) BroadcastReceiver para conexiones GATT
    private val gattUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothLeService.ACTION_GATT_CONNECTED -> {
                    Toast.makeText(this@MainActivity, "GATT Conectado", Toast.LENGTH_SHORT).show()
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                    Toast.makeText(this@MainActivity, "GATT Desconectado", Toast.LENGTH_SHORT).show()
                }
                BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED -> {
                    // Obtenemos y mostramos cuántos servicios hay
                    val services = bluetoothService?.getSupportedGattServices()
                    Toast.makeText(
                        this@MainActivity,
                        "Servicios descubiertos: ${services?.size ?: 0}",
                        Toast.LENGTH_LONG
                    ).show()
                    // Aquí podríamos navegar a otra pantalla o actualizar la UI Compose
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 3) Solicitar permiso de ubicación si es necesario
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }

        // 4) Bind al BluetoothLeService
        Intent(this, BluetoothLeService::class.java).also { intent ->
            bindService(intent, serviceConnection, BIND_AUTO_CREATE)
        }

        // 5) Registrar BroadcastReceiver para GATT updates con FLAG_NOT_EXPORTED
        registerReceiver(
            gattUpdateReceiver,
            IntentFilter().apply {
                addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
                addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
                addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
            },
            Context.RECEIVER_NOT_EXPORTED
        )

        // 6) Montar la UI Compose
        setContent {
            BLETheme {
                BLEScanScreen { device ->
                    bluetoothService?.connect(device.address)
                }
            }
        }
    }

    // 7) Manejo de la respuesta de permiso
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED)
        ) {
            Toast.makeText(
                this,
                "Necesitamos permiso de ubicación para escanear BLE",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // 8) Liberar recursos en onDestroy
    override fun onDestroy() {
        super.onDestroy()
        if (bound) {
            unbindService(serviceConnection)
            bound = false
        }
        unregisterReceiver(gattUpdateReceiver)
    }
}
