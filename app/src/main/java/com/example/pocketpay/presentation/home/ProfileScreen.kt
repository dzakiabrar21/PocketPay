package com.example.pocketpay.presentation.home

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pocketpay.data.PocketEntity
import com.example.pocketpay.data.PocketType
import com.example.pocketpay.data.TransactionEntity
import com.example.pocketpay.data.TxType
import com.example.pocketpay.presentation.scanner.QrisScannerDialog
import com.example.pocketpay.presentation.security.BiometricHelper
import com.example.pocketpay.presentation.security.ChangePinDialog
import com.example.pocketpay.presentation.security.PinManager
import com.example.pocketpay.presentation.security.PinSetupDialog
import com.example.pocketpay.presentation.security.PinResult
import com.example.pocketpay.presentation.statement.PdfStatementGenerator
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountProfileScreen(
    currentName: String,
    currentPhone: String,
    currentEmail: String,
    userTier: String,
    accountNumber: String,
    onDismiss: () -> Unit,
    onSave: (newName: String, newPhone: String, newEmail: String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var phone by remember { mutableStateOf(currentPhone) }
    var email by remember { mutableStateOf(currentEmail) }
    var isChangePinOpen by remember { mutableStateOf(false) }
    val profileContext = LocalContext.current

    if (isChangePinOpen) {
        ChangePinDialog(
            onDismiss = { isChangePinOpen = false },
            onChanged = {
                isChangePinOpen = false
                Toast.makeText(profileContext, "PIN berhasil diganti.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kelola Akun Nasabah", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Nasabah Avatar Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name.take(1),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF064E3B)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFD1FAE5)
                        ) {
                            Text(
                                text = userTier,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Informasi Pribadi
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Informasi Diri & Kontak",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF064E3B)
                        )

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nama Lengkap Nasabah") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Nomor Telepon") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Perbankan") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    onSave(name, phone, email)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF064E3B)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Simpan Perubahan", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Proteksi Keamanan (Shield Status)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Status Keamanan & Proteksi",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF064E3B)
                        )

                        SecurityStatusRow(label = "PIN Transaksi", value = "6-Digit PIN Transaksi (Aktif)")
                        SecurityStatusRow(label = "Biometrik", value = "Fingerprint / Face ID (Aktif)")
                        SecurityStatusRow(label = "Anti-Screenshot", value = "FLAG_SECURE Active (Aktif)")
                        SecurityStatusRow(label = "Session Timeout", value = "Inactivity Auto-Lock 60 Detik")

                        OutlinedButton(
                            onClick = { isChangePinOpen = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ganti PIN")
                        }
                    }
                }
            }

            // Informasi Rekening
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Informasi Rekening Perbankan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF064E3B))
                        Text("• No. Rekening Utama: $accountNumber", style = MaterialTheme.typography.bodySmall, color = Color(0xFF047857))
                        Text("• Tipe Rekening: Super Deposito PocketPay", style = MaterialTheme.typography.bodySmall, color = Color(0xFF047857))
                        Text("• Status Rekening: Aktif & Terverifikasi", style = MaterialTheme.typography.bodySmall, color = Color(0xFF047857), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityStatusRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
        }
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF064E3B))
    }
}

// -----------------------------------------------------------------------------
// ANIMATED SPLASH SCREEN COMPONENT
// -----------------------------------------------------------------------------

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
fun AccountProfileScreenPreview() {
    AccountProfileScreen(
        currentName = "Nasabah Utama",
        currentPhone = "0812-3456-7890",
        currentEmail = "nasabah@pocketpay.com",
        userTier = "Gold Premier Member",
        accountNumber = "9012839102",
        onDismiss = {},
        onSave = { _, _, _ -> }
    )
}
