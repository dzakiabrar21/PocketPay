package com.example.pocketpay.presentation.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
private fun PinField(label: String, value: String, isError: Boolean, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) onChange(it) },
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth()
    )
}

/** Mandatory first-launch dialog: the user chooses their own 6-digit PIN. Not dismissible. */
@Composable
fun PinSetupDialog(onDone: () -> Unit) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = {},
        title = { Text("Buat PIN Keamanan", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "PIN 6 digit ini dipakai untuk membuka kunci aplikasi dan mengonfirmasi pembayaran.",
                    style = MaterialTheme.typography.bodyMedium
                )
                PinField("PIN Baru", pin, error != null) { pin = it; error = null }
                PinField("Ulangi PIN", confirm, error != null) { confirm = it; error = null }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(
                enabled = pin.length == 6 && confirm.length == 6,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF064E3B)),
                onClick = {
                    if (pin != confirm) {
                        error = "Konfirmasi PIN tidak cocok."
                    } else {
                        val msg = PinManager.setInitialPin(context, pin)
                        if (msg == null) onDone() else error = msg
                    }
                }
            ) { Text("Simpan PIN") }
        }
    )
}

/** Change-PIN dialog: requires the current PIN, then a new PIN entered twice. */
@Composable
fun ChangePinDialog(onDismiss: () -> Unit, onChanged: () -> Unit) {
    val context = LocalContext.current
    var oldPin by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ganti PIN", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PinField("PIN Lama", oldPin, error != null) { oldPin = it; error = null }
                PinField("PIN Baru", pin, error != null) { pin = it; error = null }
                PinField("Ulangi PIN Baru", confirm, error != null) { confirm = it; error = null }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(
                enabled = oldPin.length == 6 && pin.length == 6 && confirm.length == 6,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF064E3B)),
                onClick = {
                    if (pin != confirm) {
                        error = "Konfirmasi PIN baru tidak cocok."
                    } else {
                        val msg = PinManager.changePin(context, oldPin, pin)
                        if (msg == null) onChanged() else { error = msg; oldPin = "" }
                    }
                }
            ) { Text("Ganti PIN") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
