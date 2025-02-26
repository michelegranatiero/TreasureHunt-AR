package com.example.treasurehunt_ar.ui.utils

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun QRCodeDisplay(code: String, sizeInDp: Float = 200f, modifier: Modifier = Modifier) {
    var qrCodeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val density = LocalDensity.current.density // Ottieni la densità corrente del dispositivo
    val sizeInPx = (sizeInDp * density).toInt() // Converti i DP in pixel
    LaunchedEffect(code) {
        qrCodeBitmap = generateQRCode(code, sizeInPx)// Esegue l'operazione in "background" (coroutine)
    }
    qrCodeBitmap?.let {
        Image(bitmap = it.asImageBitmap(), contentDescription = "QR Code", modifier = modifier)
    }
}

suspend fun generateQRCode(text: String, size: Int): Bitmap? {
    return withContext(Dispatchers.Default) { // Sposta l'operazione su un thread di background
        try {
            // val size = 512
            val bitMatrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
            Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565).apply {
                for (x in 0 until size) {
                    for (y in 0 until size) {
                        setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                    }
                }
            }
        } catch (e: WriterException) {
            e.printStackTrace()
            null
        }
    }
}

@Composable
fun QRScannerButton(text: String, onScanComplete: (String) -> Unit, modifier: Modifier = Modifier) {
    val qrScannerLauncher = rememberLauncherForActivityResult(
        contract = ScanContract()
    ) { result ->
        result.contents?.let { scannedText ->
            onScanComplete(scannedText)
        }
    }

    Button(
        onClick = {
            val options = ScanOptions().apply {
                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                setPrompt("Scan QR code")
                setBeepEnabled(false)
                setOrientationLocked(false)
                setBarcodeImageEnabled(false)
            }
            qrScannerLauncher.launch(options)
        },
        modifier = modifier,
        // colors = ButtonDefaults.buttonColors(containerColor = Color.Blue
    ) {
        Text(text = text/* , color = Color.White */)
    }
}
