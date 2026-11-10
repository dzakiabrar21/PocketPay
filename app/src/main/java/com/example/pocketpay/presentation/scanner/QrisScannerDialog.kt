package com.example.pocketpay.presentation.scanner

import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.ui.res.stringResource
import com.example.pocketpay.R
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

    // Shared by the camera and gallery paths. Camera callbacks arrive on a worker thread.
    val mainHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    fun invalidQr() {
        mainHandler.post {
            android.widget.Toast.makeText(context, context.getString(R.string.scan_qr_bukan_kode_qris_yang_valid), android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    fun handleRaw(raw: String) {
        val payload = com.example.pocketpay.domain.qris.Qris.parse(raw)
        if (payload == null) invalidQr()
        else mainHandler.post { onQrScanned(payload.merchantName, payload.amount ?: 0L) }
    }
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

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val inputImage = InputImage.fromFilePath(context, uri)
                val scanner = BarcodeScanning.getClient(
                    BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
                )
                scanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        val rawValue: String? = barcodes.firstOrNull()?.rawValue
                        if (!rawValue.isNullOrEmpty()) handleRaw(rawValue) else invalidQr()
                    }
                    .addOnFailureListener { invalidQr() }
            } catch (_: Exception) {
                invalidQr()
            }
        }
    }
    val lime = com.example.pocketpay.ui.theme.PocketTheme.colors.lime

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
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
                                QrCodeAnalyzer { qrText -> handleRaw(qrText) }
                            )
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
                            } catch (_: Exception) {}
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Soft scrims keep the controls readable over any camera image.
            Box(
                Modifier.fillMaxWidth().height(180.dp).align(Alignment.TopCenter)
                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)))
            )
            Box(
                Modifier.fillMaxWidth().height(260.dp).align(Alignment.BottomCenter)
                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
            )

            // Top bar
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                com.example.pocketpay.ui.components.CircleIcon(
                    androidx.compose.material.icons.Icons.AutoMirrored.Rounded.ArrowBack,
                    stringResource(R.string.profile_kembali),
                    container = Color.White.copy(alpha = 0.18f), tint = Color.White, onClick = onDismiss
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    stringResource(R.string.scan_title), color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }

            if (hasCameraPermission) {
                // Viewfinder: four lime corner brackets
                androidx.compose.foundation.Canvas(Modifier.size(260.dp).align(Alignment.Center)) {
                    val len = 56.dp.toPx(); val w = 6.dp.toPx(); val r = 22.dp.toPx()
                    val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = w, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    fun corner(x: Float, y: Float, dx: Float, dy: Float) {
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(x, y + dy * len); lineTo(x, y + dy * r)
                            quadraticTo(x, y, x + dx * r, y); lineTo(x + dx * len, y)
                        }
                        drawPath(path, lime, style = stroke)
                    }
                    corner(0f, 0f, 1f, 1f); corner(size.width, 0f, -1f, 1f)
                    corner(0f, size.height, 1f, -1f); corner(size.width, size.height, -1f, -1f)
                }
                Text(
                    stringResource(R.string.scan_hint), color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(top = 320.dp).padding(horizontal = 40.dp)
                )
            } else {
                Text(
                    text = stringResource(R.string.scan_izin_akses_kamera_dibutuhkan_untuk_m),
                    color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(32.dp)
                )
            }

            // Bottom actions
            Row(
                Modifier.fillMaxWidth().align(Alignment.BottomCenter).navigationBarsPadding().padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                com.example.pocketpay.ui.components.PillButton(
                    stringResource(R.string.scan_pilih_gambar_qr_dari_galeri), { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f), icon = androidx.compose.material.icons.Icons.Rounded.PhotoLibrary,
                    style = com.example.pocketpay.ui.components.PillStyle.SOFT_ON_HERO, height = 54.dp
                )
                com.example.pocketpay.ui.components.PillButton(
                    stringResource(R.string.scan_input_merchant_manual_emulator), onManualInputFallback,
                    modifier = Modifier.weight(1f), icon = androidx.compose.material.icons.Icons.Rounded.Edit, height = 54.dp
                )
            }
        }
    }
}
