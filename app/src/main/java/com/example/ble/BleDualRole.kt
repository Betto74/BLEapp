package com.example.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import java.util.*

object BleConstants {
    val SERVICE_UUID        = UUID.fromString("0000feed-0000-1000-8000-00805f9b34fb")
    val CHARACTERISTIC_UUID = UUID.fromString("0000beef-0000-1000-8000-00805f9b34fb")
    val CCC_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    const val TAG = "BLEManager"
}

// ----------------------------------------
// Rol Periférico: GATT Server + Advertising
// ----------------------------------------
@SuppressLint("MissingPermission")
class BlePeripheralManager(private val context: Context) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val advertiser: BluetoothLeAdvertiser? = bluetoothAdapter.bluetoothLeAdvertiser
    private var gattServer: BluetoothGattServer? = null
    private val connectedDevices = mutableSetOf<BluetoothDevice>()

    /** Callback que la UI puede asignar para recibir mensajes entrantes */
    var onMessageReceived: (String) -> Unit = {}

    fun start() {
        startGattServer()
        startAdvertising()
    }

    fun stop() {
        stopAdvertising()
        stopGattServer()
    }

    fun sendMessage(message: String) {
        val svc = gattServer?.getService(BleConstants.SERVICE_UUID) ?: return
        val char = svc.getCharacteristic(BleConstants.CHARACTERISTIC_UUID) ?: return
        char.value = message.toByteArray()
        connectedDevices.forEach { device ->
            gattServer?.notifyCharacteristicChanged(device, char, false)
        }
    }

    private fun startGattServer() {
        gattServer = bluetoothManager.openGattServer(context, gattServerCallback)
        val service = BluetoothGattService(
            BleConstants.SERVICE_UUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )
        val char = BluetoothGattCharacteristic(
            BleConstants.CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        val descriptor = BluetoothGattDescriptor(
            BleConstants.CCC_DESCRIPTOR_UUID,
            BluetoothGattDescriptor.PERMISSION_WRITE
        )
        char.addDescriptor(descriptor)
        service.addCharacteristic(char)
        gattServer?.addService(service)
        Log.i(BleConstants.TAG, "Peripheral GATT Server started")
    }

    private fun stopGattServer() {
        gattServer?.close()
        gattServer = null
        connectedDevices.clear()
        Log.i(BleConstants.TAG, "Peripheral GATT Server stopped")
    }

    private fun startAdvertising() {
        advertiser?.let { adv ->
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(true)
                .setTimeout(0)
                .build()
            val data = AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID))
                .build()
            adv.startAdvertising(settings, data, advertiseCallback)
        }
    }

    private fun stopAdvertising() {
        advertiser?.stopAdvertising(advertiseCallback)
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectedDevices.add(device)
                Log.i(BleConstants.TAG, "Peripheral: device connected ${device.address}")
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connectedDevices.remove(device)
                Log.i(BleConstants.TAG, "Peripheral: device disconnected ${device.address}")
            }
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice, requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean, responseNeeded: Boolean,
            offset: Int, value: ByteArray
        ) {
            if (characteristic.uuid == BleConstants.CHARACTERISTIC_UUID) {
                val msg = String(value)
                Log.i(BleConstants.TAG, "Peripheral received: $msg")
                onMessageReceived(msg)  // <-- notificamos a la UI
                // Echo back
                characteristic.value = "Echo: $msg".toByteArray()
                gattServer?.notifyCharacteristicChanged(device, characteristic, false)
            }
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
            }
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.i(BleConstants.TAG, "Peripheral advertising started")
        }
        override fun onStartFailure(errorCode: Int) {
            Log.w(BleConstants.TAG, "Peripheral advertising failed: $errorCode")
        }
    }
}

// ----------------------------------------
// Rol Central: Central Manager
// ----------------------------------------
@SuppressLint("MissingPermission")
class BleCentralManager(private val context: Context) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val scanner = bluetoothAdapter.bluetoothLeScanner
    private var bluetoothGatt: BluetoothGatt? = null
    private val handler = Handler(Looper.getMainLooper())

    var onDeviceFound: (BluetoothDevice) -> Unit = {}
    var onMessageReceived: (String) -> Unit = {}

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            onDeviceFound(result.device)
        }
    }

    fun startScan() {
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .build()
        scanner.startScan(listOf(filter), settings, scanCallback)
        handler.postDelayed({ stopScan() }, 10_000)
    }

    fun stopScan() {
        scanner.stopScan(scanCallback)
    }

    fun connect(device: BluetoothDevice) {
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    fun sendMessage(text: String) {
        val gatt = bluetoothGatt ?: return
        val svc = gatt.getService(BleConstants.SERVICE_UUID) ?: return
        val char = svc.getCharacteristic(BleConstants.CHARACTERISTIC_UUID) ?: return
        char.value = text.toByteArray()
        gatt.writeCharacteristic(char)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices()
            }
        }
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val svc = gatt.getService(BleConstants.SERVICE_UUID) ?: return
            val char = svc.getCharacteristic(BleConstants.CHARACTERISTIC_UUID)
            gatt.setCharacteristicNotification(char, true)
            val desc = char.getDescriptor(BleConstants.CCC_DESCRIPTOR_UUID)
            desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(desc)
        }
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            onMessageReceived(String(characteristic.value ?: byteArrayOf()))
        }
    }
}
