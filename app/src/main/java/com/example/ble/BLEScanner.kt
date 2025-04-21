package com.example.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.BluetoothLeScanner
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateListOf


@SuppressLint("MissingPermission")
object BLEScanner {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    private var scanning = false
    private val handler = Handler(Looper.getMainLooper())
    private const val SCAN_PERIOD: Long = 10_000L

    // Lista observable para Compose
    val devices = mutableStateListOf<BluetoothDevice>()

    // Callback que añade cada device a la lista
    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            result.device?.let { device ->
                if (!devices.contains(device)) {
                    devices += device
                }
            }
        }
    }

    // Función que inicia/detiene el escaneo con límite de tiempo


    fun scanLeDevice() {
        bluetoothLeScanner ?: return
        if (!scanning) {
            handler.postDelayed({
                scanning = false
                bluetoothLeScanner.stopScan(leScanCallback)
            }, SCAN_PERIOD)
            scanning = true
            bluetoothLeScanner.startScan(leScanCallback)
        } else {
            scanning = false
            bluetoothLeScanner.stopScan(leScanCallback)
        }
    }
}
