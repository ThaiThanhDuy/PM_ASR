package com.example.asr

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

@Composable
fun LoginScreen(navController: NavHostController) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoginSuccessful by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf("") }
    var isLoggingIn by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonColor = if (isPressed) Color(106, 255, 153) else MaterialTheme.colorScheme.primary
    var showDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Đăng Nhập", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 16.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Tên đăng nhập") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mật khẩu") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // Mock login logic with specific credentials
                isLoggingIn = true
                loginError = ""
                if (username == "UTE" && password == "123") {
                    isLoginSuccessful = true
                    // In a real app, you'd likely call an authentication service here
                    // and navigate upon successful response.
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true } // Prevent going back to login
                    }
                } else {
                    loginError = "Tên đăng nhập hoặc mật khẩu không đúng."
                    isLoginSuccessful = false
                }
                isLoggingIn = false
            },
            enabled = !isLoggingIn,
            interactionSource = interactionSource,
            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
            modifier = Modifier.fillMaxWidth() // Make the button fill the width
        ) {
            Text(if (isLoggingIn) "Đang đăng nhập..." else "Đăng Nhập")
        }

        Spacer(modifier = Modifier.height(8.dp))

        ClickableText(
            text = AnnotatedString("Quên mật khẩu?"),
            onClick = { showDialog = true },
            style = TextStyle(
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline
            )
        )

        if (loginError.isNotEmpty()) {
            Text(loginError, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Quên mật khẩu") },
                text = { Text("Vui lòng liên hệ SĐT : 0817747111") },
                confirmButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Đóng")
                    }
                }
            )
        }
    }
}