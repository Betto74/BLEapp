package com.example.ble

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    enum class Role { NONE, PERIPHERAL, CENTRAL }

    private lateinit var blePeripheralManager: BlePeripheralManager
    private lateinit var bleCentralManager: BleCentralManager
    private var role by mutableStateOf(Role.NONE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        blePeripheralManager = BlePeripheralManager(this)
        bleCentralManager    = BleCentralManager(this)

        setContent {
            when (role) {
                Role.NONE -> RoleSelectionScreen(
                    onPeripheral = { role = Role.PERIPHERAL; blePeripheralManager.start() },
                    onCentral    = { role = Role.CENTRAL    /* no startScan aquí */ }
                )
                Role.PERIPHERAL -> PeripheralChatScreen(blePeripheralManager)
                Role.CENTRAL    -> CentralDeviceScanScreen(bleCentralManager)
            }
        }
    }
}

@Composable
fun RoleSelectionScreen(onPeripheral: () -> Unit, onCentral: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = onPeripheral, modifier = Modifier.fillMaxWidth()) {
            Text("Ser Periférico")
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onCentral, modifier = Modifier.fillMaxWidth()) {
            Text("Ser Central")
        }
    }
}

@Composable
fun PeripheralChatScreen(blePeripheral: BlePeripheralManager) {
    var message by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        blePeripheral.onMessageReceived = { msg ->
            messages += "Ellos: $msg"
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        LazyColumn(Modifier.weight(1f)) {
            items(messages) { Text(it) }
        }
        Row(Modifier.fillMaxWidth().padding(8.dp)) {
            BasicTextField(
                value = message,
                onValueChange = { message = it },
                modifier = Modifier.weight(1f)
            )
            Button(onClick = {
                blePeripheral.sendMessage(message)
                messages += "Me: $message"
                message = ""
            }) {
                Text("Enviar")
            }
        }
    }
}

@Suppress("MissingPermission")
@Composable
fun CentralDeviceScanScreen(bleCentral: BleCentralManager) {
    var devices by remember { mutableStateOf(listOf<BluetoothDevice>()) }
    var connected by remember { mutableStateOf(false) }
    var showChat by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        bleCentral.onDeviceFound = { dev -> devices = devices + dev }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        if (!connected && !showChat) {
            Button(onClick = { bleCentral.startScan() }) {
                Text("Buscar Periférico")
            }
            Spacer(Modifier.height(16.dp))
            LazyColumn {
                items(devices) { device ->
                    Text(
                        text = "${device.name ?: "Desconocido"} — ${device.address}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                bleCentral.connect(device)
                                connected = true
                                showChat = true
                            }
                            .padding(8.dp)
                    )
                }
            }
        } else if (showChat) {
            ChatScreen(bleCentral)
        }
    }
}

@Composable
fun ChatScreen(bleCentral: BleCentralManager) {
    var message by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        bleCentral.onMessageReceived = { msg ->
            messages += "Ellos: $msg"
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        LazyColumn(Modifier.weight(1f)) {
            items(messages) { Text(it) }
        }
        Row(Modifier.fillMaxWidth().padding(8.dp)) {
            BasicTextField(
                value = message,
                onValueChange = { message = it },
                modifier = Modifier.weight(1f)
            )
            Button(onClick = {
                bleCentral.sendMessage(message)
                messages += "Me: $message"
                message = ""
            }) {
                Text("Enviar")
            }
        }
    }
}
