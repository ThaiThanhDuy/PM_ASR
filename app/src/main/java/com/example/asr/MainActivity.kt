package com.example.asr

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.asr.ui.theme.ASRTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the MQTT helper here (can be moved later if needed)
        val cameraBitmap = mutableStateOf<Bitmap?>(null)
        val mqttHelper = MqttHelper(onImageReceived = { imageBytes ->
            cameraBitmap.value = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        })
        mqttHelper.connect()

        setContent {
            ASRTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "login") {
                    composable("login") { LoginScreen(navController = navController) }
                    composable("main") {
                        MainScreen(
                            mqttHelper = mqttHelper,
                            cameraBitmap = cameraBitmap
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(mqttHelper: MqttHelper, cameraBitmap: MutableState<Bitmap?>) {
    val selectedTabIndex = remember { mutableStateOf(0) }

    Scaffold { paddingValues ->
        Column(Modifier.padding(paddingValues)) {
            TabRow(selectedTabIndex = selectedTabIndex.value) {
                Tab(
                    selected = selectedTabIndex.value == 0,
                    onClick = {
                        selectedTabIndex.value = 0
                        println("Tab Điều khiển được chọn")
                    },
                    text = {
                        Text(
                            "Điều khiển",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex.value == 1,
                    onClick = {
                        selectedTabIndex.value = 1
                        println("Tab Camera được chọn")
                    },
                    text = {
                        Text(
                            "Camera",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex.value == 2,
                    onClick = {
                        selectedTabIndex.value = 2
                        println("Tab GPS được chọn")
                    },
                    text = {
                        Text(
                            "GPS",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                )
            }

            when (selectedTabIndex.value) {
                0 -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(id = R.drawable.robotletan),
                            contentDescription = "Background Image",
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(0.5f)
                                .offset { IntOffset(x = -50, y = 100) },
                            contentScale = ContentScale.Crop
                        )
                        ControlPanelTab(mqttHelper = mqttHelper, modifier = Modifier.fillMaxSize())
                    }
                }

                1 -> CameraTab(cameraBitmap = cameraBitmap)
                2 -> GPSTab()
            }
        }
    }
}

@Composable
fun ControlPanelTab(mqttHelper: MqttHelper, modifier: Modifier = Modifier) {
    val connectionStatus = mqttHelper.connectionStatus.value
    val commandStatus = remember { mutableStateOf("") }
    val velocity = remember { mutableFloatStateOf(0f) }
    val sliderColor = Color(106, 255, 153)

    Column(
        modifier = modifier
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Connection Status: ${if (connectionStatus) "Connected" else "Disconnected"}",
            modifier = Modifier.padding(bottom = 8.dp),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
        )

        if (commandStatus.value.isNotEmpty()) {
            Text(
                text = commandStatus.value,
                modifier = Modifier.padding(bottom = 8.dp),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ControlButton(text = "Move Forward", onClick = {
                mqttHelper.publishCommand("FORWARD-${velocity.value.toInt()}")
                commandStatus.value = "Đã gửi lệnh: FORWARD-${velocity.value.toInt()}%"
            })

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ControlButton(text = "Move Left", onClick = {
                    mqttHelper.publishCommand("LEFT-${velocity.value.toInt()}")
                    commandStatus.value = "Đã gửi lệnh: LEFT-${velocity.value.toInt()}%"
                })

                ControlButton(text = "Stop", onClick = {
                    mqttHelper.publishCommand("STOP")
                    commandStatus.value = "Đã gửi lệnh: STOP"
                })

                ControlButton(text = "Move Right", onClick = {
                    mqttHelper.publishCommand("RIGHT-${velocity.value.toInt()}")
                    commandStatus.value = "Đã gửi lệnh: RIGHT-${velocity.value.toInt()}%"
                })
            }

            Spacer(modifier = Modifier.height(8.dp))

            ControlButton(text = "Move Backward", onClick = {
                mqttHelper.publishCommand("BACKWARD-${velocity.value.toInt()}")
                commandStatus.value = "Đã gửi lệnh: BACKWARD-${velocity.value.toInt()}%"
            })
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Vận tốc: ${velocity.value.toInt()}%",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
        )
        Slider(
            value = velocity.value,
            onValueChange = { velocity.value = it },
            valueRange = 0f..100f,
            steps = 99,
            colors = SliderDefaults.colors(
                thumbColor = sliderColor,
                activeTrackColor = sliderColor,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                activeTickColor = sliderColor,
                inactiveTickColor = MaterialTheme.colorScheme.surface
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            ControlButton(text = "HOME", onClick = {
                mqttHelper.publishCommand("HOME")
                commandStatus.value = "Đã gửi lệnh: HOME"
            })
            Spacer(modifier = Modifier.weight(1f)) // Pushes the DOCK button to the right
            ControlButton(text = "DOCK", onClick = {
                mqttHelper.publishCommand("DOCK")
                commandStatus.value = "Đã gửi lệnh: DOCK"
            })
        }
    }
}

@Composable
fun ControlButton(text: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonColor = if (isPressed) Color(106, 255, 153) else MaterialTheme.colorScheme.primary

    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
fun CameraTab(cameraBitmap: MutableState<Bitmap?>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Luồng camera từ xe sẽ hiển thị ở đây.",
            modifier = Modifier.padding(bottom = 16.dp),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
        )
        if (cameraBitmap.value != null) {
            Image(
                bitmap = cameraBitmap.value!!.asImageBitmap(),
                contentDescription = "Camera Stream",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp) // Tăng kích thước hiển thị camera
            )
        } else {
            Text(
                "Đang chờ luồng camera...",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
fun GPSTab() {
    val context = LocalContext.current
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val location = remember { mutableStateOf<Location?>(null) }
    val hasGpsPermission = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    DisposableEffect(context) {
        val locationListener = object : LocationListener {
            override fun onLocationChanged(newLocation: Location) {
                location.value = newLocation
            }

            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }

        if (hasGpsPermission.value) {
            try {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000,
                    1f,
                    locationListener
                )
            } catch (e: SecurityException) {
                hasGpsPermission.value = false
            }
        }

        onDispose {
            locationManager.removeUpdates(locationListener)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Thông tin GPS",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (hasGpsPermission.value) {
            if (location.value != null) {
                Text(
                    text = "Latitude: ${location.value!!.latitude}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Longitude: ${location.value!!.longitude}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Accuracy: ${location.value!!.accuracy} m",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                location.value?.altitude?.let {
                    Text(
                        text = "Altitude: $it m",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                location.value?.speed?.let {
                    Text(
                        text = "Speed: $it m/s",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            } else {
                Text(
                    text = "Đang chờ tín hiệu GPS...",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        } else {
            Text(
                text = "Ứng dụng chưa có quyền truy cập GPS.",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
            Button(onClick = {
                ActivityCompat.requestPermissions(
                    context as MainActivity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    100
                )
            }) {
                Text(
                    "Yêu cầu quyền GPS",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ASRTheme {
        val cameraBitmap = remember { mutableStateOf<Bitmap?>(null) }
        val mqttHelper = remember {
            MqttHelper(onImageReceived = { }).apply { connect() }
        }
        MainScreen(mqttHelper = mqttHelper, cameraBitmap = cameraBitmap)
    }
}