package com.example.asr

import android.graphics.BitmapFactory
import android.util.Log
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.MqttGlobalPublishFilter
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

class MqttHelper(
    private val onImageReceived: (ByteArray) -> Unit // Callback để thông báo khi nhận ảnh
) {

    val connectionStatus = mutableStateOf(false)
    private val client: Mqtt3AsyncClient = MqttClient.builder()
        .useMqttVersion3()
        .serverHost("c69aeca2d48441618b65f77f38e2d8dc.s1.eu.hivemq.cloud")
        .serverPort(8883)
        .sslWithDefaultConfig()
        .identifier("android-client-" + System.currentTimeMillis())
        .buildAsync()

    fun connect() {
        val connectFuture: CompletableFuture<Mqtt3ConnAck> = client.connectWith()
            .simpleAuth()
            .username("hivemq.webclient.1746280264795")
            .password("ay;Z9W0\$L1k7fbS!xH?C".toByteArray(Charsets.UTF_8))
            .applySimpleAuth()
            .send()

        connectFuture.whenComplete { connAck, throwable ->
            if (throwable != null) {
                Log.e("MQTT", " Kết nối thất bại: ${throwable.message}", throwable)
                connectionStatus.value = false
            } else {
                Log.i("MQTT", " Đã kết nối thành công tới MQTT broker.")
                connectionStatus.value = true
                subscribeAll()
            }
        }
    }

    private fun subscribeAll() {
        client.publishes(MqttGlobalPublishFilter.ALL) { publish ->
            when (publish.topic.toString()) {
                "command/car" -> {
                    val message = String(publish.payloadAsBytes, StandardCharsets.UTF_8)
                    Log.i("MQTT", "Nhận lệnh từ topic '${publish.topic}': $message")
                    // Xử lý lệnh ở đây nếu cần trong MqttHelper
                }
                "camera/stream" -> {
                    val imageBytes = publish.payloadAsBytes
                    Log.i("MQTT", "Nhận ảnh từ topic '${publish.topic}': ${imageBytes.size} bytes")
                    onImageReceived(imageBytes) // Gọi callback khi nhận được ảnh
                }
                else -> {
                    Log.i("MQTT", "Nhận dữ liệu không xác định từ topic '${publish.topic}'")
                }
            }
        }

        client.subscribeWith()
            .topicFilter("command/car")
            .send()
            .whenComplete { subAck, throwable ->
                if (throwable != null) {
                    Log.e("MQTT", " Lỗi khi subscribe vào 'command/car': ${throwable.message}", throwable)
                } else {
                    Log.i("MQTT", " Đã subscribe thành công vào topic 'command/car'")
                }
            }

        client.subscribeWith()
            .topicFilter("camera/stream")
            .send()
            .whenComplete { subAck, throwable ->
                if (throwable != null) {
                    Log.e("MQTT", " Lỗi khi subscribe vào 'camera/stream': ${throwable.message}", throwable)
                } else {
                    Log.i("MQTT", " Đã subscribe thành công vào topic 'camera/stream'")
                }
            }
    }

    fun publishCommand(command: String) {
        client.publishWith()
            .topic("command/car")
            .payload(command.toByteArray(StandardCharsets.UTF_8))
            .send()
            .whenComplete { _, throwable ->
                if (throwable != null) {
                    Log.e("MQTT", " Gửi lệnh thất bại: ${throwable.message}", throwable)
                } else {
                    Log.i("MQTT", " Đã gửi lệnh: $command")
                }
            }
    }
}