package com.kanso.feature.utilities

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kanso.core.ui.components.KansoBentoCard
import com.kanso.core.ui.components.KansoButtonSize
import com.kanso.core.ui.components.KansoPrimaryButton
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun QrScannerCard(
    onScanned: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    if (!hasCameraPermission) {
        KansoBentoCard(
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.QrCodeScanner,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Camera permission needed", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Kanso scans QR codes 100% offline on-device using CameraX.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                KansoPrimaryButton(
                    text = "Grant Camera Access",
                    size = KansoButtonSize.LARGE,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                )
            }
        }
        return
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val delivered = remember { AtomicBoolean(false) }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(lifecycleOwner, previewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        var boundProvider: ProcessCameraProvider? = null

        cameraProviderFuture.addListener(
            {
                val provider = runCatching { cameraProviderFuture.get() }.getOrNull() ?: return@addListener
                boundProvider = provider

                val preview = Preview.Builder().build().apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }

                val analyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .apply {
                        setAnalyzer(analysisExecutor) { imageProxy ->
                            try {
                                if (!delivered.get()) {
                                    val yBytes = extractTightYPlane(imageProxy)
                                    val decoded = QrCodeUtils.decodeLuminanceFrame(
                                        yBytes = yBytes,
                                        width = imageProxy.width,
                                        height = imageProxy.height
                                    )
                                    if (!decoded.isNullOrBlank() && delivered.compareAndSet(false, true)) {
                                        mainExecutor.execute { onScanned(decoded) }
                                    }
                                }
                            } finally {
                                imageProxy.close()
                            }
                        }
                    }

                runCatching {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analyzer
                    )
                }
            },
            mainExecutor
        )

        onDispose {
            runCatching { boundProvider?.unbindAll() }
            analysisExecutor.shutdown()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(20.dp))
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun extractTightYPlane(imageProxy: ImageProxy): ByteArray {
    val plane = imageProxy.planes[0]
    val buffer = plane.buffer
    val width = imageProxy.width
    val height = imageProxy.height
    val rowStride = plane.rowStride

    buffer.rewind()
    if (rowStride == width) {
        val out = ByteArray(width * height)
        buffer.get(out, 0, out.size.coerceAtMost(buffer.remaining()))
        return out
    }

    val out = ByteArray(width * height)
    val rowBuffer = ByteArray(rowStride)
    for (row in 0 until height) {
        val toRead = minOf(rowStride, buffer.remaining())
        if (toRead <= 0) break
        buffer.get(rowBuffer, 0, toRead)
        System.arraycopy(rowBuffer, 0, out, row * width, minOf(width, toRead))
    }
    return out
}
