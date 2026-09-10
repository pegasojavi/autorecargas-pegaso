package com.autorecargaspegaso.feature.qrscanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * Escáner QR (CLAUDE.md sección 2/5/10): decodifica el QR físico del
 * cargador y llama a [onQrDetected] con el contenido crudo — la
 * interpretación (qué proveedor es, si hace falta ir a `ProviderDirectory`)
 * es responsabilidad del llamador, no de este módulo.
 *
 * No graba ni almacena imágenes; el análisis de fotogramas es en vivo y se
 * descarta al momento (CLAUDE.md sección 10).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerScreen(
    onQrDetected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { granted -> hasCameraPermission = granted }

    Scaffold(
        modifier = modifier,
        topBar = {
            // A petición del usuario: título visible arriba de la ventana de
            // escaneo, para dejar claro qué se está apuntando con la cámara.
            CenterAlignedTopAppBar(title = { Text(stringResource(R.string.qrscanner_title)) })
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            if (hasCameraPermission) {
                CameraPreviewWithAnalysis(onQrDetected = onQrDetected)
            } else {
                PermissionRationale(onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) })
            }
        }
    }
}

@Composable
private fun PermissionRationale(onRequestPermission: () -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(24.dp)) {
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.qrscanner_permission_rationale),
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(onClick = onRequestPermission, modifier = Modifier.padding(top = 16.dp)) {
                Text(stringResource(R.string.qrscanner_permission_action))
            }
        }
    }
}

@Composable
private fun CameraPreviewWithAnalysis(onQrDetected: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnQrDetected by rememberUpdatedState(onQrDetected)
    var alreadyReported by remember { mutableStateOf(false) }

    val previewView = remember { PreviewView(context) }

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build(),
        )

        cameraProviderFuture.addListener(
            {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null && !alreadyReported) {
                        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        scanner.process(inputImage)
                            .addOnSuccessListener { barcodes ->
                                val value = barcodes.firstOrNull()?.rawValue
                                if (value != null && !alreadyReported) {
                                    alreadyReported = true
                                    currentOnQrDetected(value)
                                }
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    } else {
                        imageProxy.close()
                    }
                }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis,
                )
            },
            ContextCompat.getMainExecutor(context),
        )

        onDispose {
            cameraProviderFuture.get().unbindAll()
            scanner.close()
        }
    }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}
