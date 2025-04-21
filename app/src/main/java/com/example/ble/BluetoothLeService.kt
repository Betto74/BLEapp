package com.example.ble

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.*
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log


@SuppressLint("MissingPermission")
class BluetoothLeService : Service() {

    companion object {
        private const val TAG = "BluetoothLeService"

        const val ACTION_GATT_CONNECTED =
            "com.example.ble.ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED =
            "com.example.ble.ACTION_GATT_DISCONNECTED"
        // Nuevo broadcast para servicios descubiertos
        const val ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.ble.ACTION_GATT_SERVICES_DISCOVERED"

        private const val STATE_DISCONNECTED = 0
        private const val STATE_CONNECTED = 2
    }

    private val binder = LocalBinder()
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var connectionState = STATE_DISCONNECTED

    inner class LocalBinder : Binder() {
        fun getService(): BluetoothLeService = this@BluetoothLeService
    }

    override fun onBind(intent: Intent?): IBinder? = binder

    override fun onUnbind(intent: Intent?): Boolean {
        close()
        return super.onUnbind(intent)
    }

    fun initialize(): Boolean {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        return bluetoothAdapter != null
    }


    fun connect(address: String): Boolean {
        bluetoothAdapter?.let { adapter ->
            return try {
                val device = adapter.getRemoteDevice(address)
                bluetoothGatt = device.connectGatt(this, false, gattCallback)
                true
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Device not found", e)
                false
            }
        } ?: run {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return false
        }
    }

    private fun close() {
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    private fun broadcastUpdate(action: String) {
        sendBroadcast(Intent(action))
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int
        ) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectionState = STATE_CONNECTED
                Log.i(TAG, "Connected to GATT server.")
                broadcastUpdate(ACTION_GATT_CONNECTED)
                // ¡Aquí lanzamos descubrimiento de servicios!
                bluetoothGatt?.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connectionState = STATE_DISCONNECTED
                Log.i(TAG, "Disconnected from GATT server.")
                broadcastUpdate(ACTION_GATT_DISCONNECTED)
            }
        }

        // ------------------------------------------------------
        // Nueva sección: manejamos servicios descubiertos
        override fun onServicesDiscovered(
            gatt: BluetoothGatt,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Services discovered.")
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
            } else {
                Log.w(TAG, "onServicesDiscovered received: $status")
            }
        }
        // ------------------------------------------------------
    }

    /** Permite obtener la lista tras discovery */
    fun getSupportedGattServices(): List<BluetoothGattService>? =
        bluetoothGatt?.services
}
