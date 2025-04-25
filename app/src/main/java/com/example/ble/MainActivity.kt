package com.example.ble

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {
    private lateinit var blePeripheralManager: BlePeripheralManager
    private lateinit var bleCentralManager: BleCentralManager
    enum class Role { NONE, PERIPHERAL, CENTRAL }
    private var role by mutableStateOf(Role.NONE)

    private val permissionLauncher = registerForActivityResult(RequestMultiplePermissions()) { permissions ->
        // Manejar resultados de permisos si es necesario
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Solicitar permisos BLE y ubicación al iniciar
        permissionLauncher.launch(arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))

        blePeripheralManager = BlePeripheralManager(this)
        bleCentralManager    = BleCentralManager(this)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                    ) {
                        when (role) {
                            Role.NONE -> RoleSelectionScreen()
                            Role.PERIPHERAL -> PeripheralChatScreen(blePeripheralManager)
                            Role.CENTRAL -> CentralDeviceScanScreen(bleCentralManager)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun RoleSelectionScreen() {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    role = Role.PERIPHERAL
                    blePeripheralManager.start()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("Ser Periférico")
            }
            Button(
                onClick = { role = Role.CENTRAL },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("Ser Central")
            }
        }
    }

    @Composable
    fun PeripheralChatScreen(blePeripheral: BlePeripheralManager) {
        var message by remember { mutableStateOf(TextFieldValue("")) }
        val messages = remember { mutableStateListOf<String>() }

        LaunchedEffect(Unit) {
            blePeripheral.onMessageReceived = { msg ->
                messages += "Ellos: $msg"
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(messages) { msg ->
                    val isMe = msg.startsWith("Me:")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                    ) {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = msg.removePrefix("Me: ").removePrefix("Ellos: "),
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    placeholder = { Text("Escribe un mensaje...") }
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        blePeripheral.sendMessage(message.text)
                        messages += "Me: ${message.text}"
                        message = TextFieldValue("")
                    }
                ) {
                    Text("Enviar")
                }
            }
        }
    }

    @Composable
    @Suppress("MissingPermission")
    fun CentralDeviceScanScreen(bleCentral: BleCentralManager) {
        var devices by remember { mutableStateOf(listOf<BluetoothDevice>()) }
        var connected by remember { mutableStateOf(false) }
        var showChat by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            bleCentral.onDeviceFound = { dev ->
                if (!devices.contains(dev)) devices = devices + dev
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (!connected && !showChat) {
                Button(
                    onClick = { bleCentral.startScan() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text("Buscar Periférico")
                }
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
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
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
        val messages = remember { mutableStateListOf<String>() }

        LaunchedEffect(Unit) {
            bleCentral.onMessageReceived = { msg ->
                messages += "Ellos: $msg"
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(messages) { msg ->
                    val isMe = msg.startsWith("Me:")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                    ) {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = msg.removePrefix("Me: ").removePrefix("Ellos: "),
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
