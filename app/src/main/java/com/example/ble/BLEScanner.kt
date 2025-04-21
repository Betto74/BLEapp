package com.example.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.BluetoothLeScanner
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
@SuppressLint("MissingPermission")
object BLEScanner {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    private val handler = Handler(Looper.getMainLooper())
    private const val SCAN_PERIOD: Long = 10_000L

    // Lista observable para dispositivos
    val devices = mutableStateListOf<BluetoothDevice>()

    // Estado observable para si estamos escaneando
    val isScanning: MutableState<Boolean> = mutableStateOf(false)

    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            result.device?.let { device ->
                if (!devices.contains(device)) {
                    devices += device
                }
            }
        }
    }


    fun scanLeDevice() {
        bluetoothLeScanner ?: return

        if (!isScanning.value) {
            // limpio lista
            devices.clear()
            // detengo tras SCAN_PERIOD
            handler.postDelayed({
                bluetoothLeScanner.stopScan(leScanCallback)
                isScanning.value = false
            }, SCAN_PERIOD)

            isScanning.value = true
            bluetoothLeScanner.startScan(leScanCallback)

        } else {
            bluetoothLeScanner.stopScan(leScanCallback)
            isScanning.value = false
        }
    }
}
