package com.example.pocketpay.presentation.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun QrisScannerDialog(
    onDismiss: () -> Unit,
    onQrScanned: (rawMerchant: String, estimatedAmount: Long) -> Unit,
    onManualInputFallback: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (hasCameraPermission) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            imageAnalysis.setAnalyzer(
                                Executors.newSingleThreadExecutor(),
                                QrCodeAnalyzer { qrText ->
                                    val parts = qrText.split("|")
                                    val merchant = parts.getOrNull(0) ?: qrText
                                    val amount = parts.getOrNull(1)?.toLongOrNull() ?: 50_000L

                                    onQrScanned(merchant, amount)
                                }
                            )

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (_: Exception) {}
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Kotak Bidik Scanner
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                        .align(Alignment.Center)
                )
            } else {
                Text(
                    text = "Izin akses kamera dibutuhkan untuk memindai QRIS.",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Tombol Kontrol Bawah
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val galleryLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri ->
                    if (uri != null) {
                        try {
                            val inputImage = InputImage.fromFilePath(context, uri)
                            val scanner = BarcodeScanning.getClient(
                                BarcodeScannerOptions.Builder()
                                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                                    .build()
                            )
                            scanner.process(inputImage)
                                .addOnSuccessListener { barcodes ->
                                    val rawValue: String? = barcodes.firstOrNull()?.rawValue
                                    if (rawValue != null && rawValue.isNotEmpty()) {
                                        val parts = rawValue.split("|")
                                        val merchant = parts.getOrNull(0) ?: rawValue
                                        val amount = parts.getOrNull(1)?.toLongOrNull() ?: 50_000L
                                        onQrScanned(merchant, amount)
                                    } else {
                                        onQrScanned("Merchant QRIS Galeri", 50_000L)
                                    }
                                }
                                .addOnFailureListener {
                                    onQrScanned("Merchant QRIS Galeri", 50_000L)
                                }
                        } catch (_: Exception) {
                            onQrScanned("Merchant QRIS Galeri", 50_000L)
                        }
                    }
                }

                Button(
                    onClick = { galleryLauncher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Pilih Gambar QR dari Galeri")
                }

                OutlinedButton(
                    onClick = onManualInputFallback,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Input Merchant Manual (Emulator)")
                }

                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                ) {
                    Text("Tutup Scanner")
                }
            }
        }
    }
}