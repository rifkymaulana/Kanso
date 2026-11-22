package com.kanso.feature.utilities

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.EncodeHintType

object QrCodeUtils {

    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap? {
        if (content.isBlank() || sizePx <= 0) return null
        return runCatching {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 2
            )
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val width = matrix.width
            val height = matrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (matrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, width, 0, 0, width, height)
            }
        }.getOrNull()
    }

    fun decodeLuminanceFrame(yBytes: ByteArray, width: Int, height: Int): String? {
        if (yBytes.size < width * height || width <= 0 || height <= 0) return null
        return runCatching {
            val source = PlanarYUVLuminanceSource(
                yBytes,
                width,
                height,
                0,
                0,
                width,
                height,
                false
            )
            val bitmap = BinaryBitmap(HybridBinarizer(source))
            val hints = mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                DecodeHintType.TRY_HARDER to true
            )
            MultiFormatReader().decode(bitmap, hints).text
        }.getOrNull()
    }

    fun buildWifiQrPayload(ssid: String, password: String, security: String = "WPA"): String {
        val escapedSsid = escapeWifiField(ssid.trim())
        val escapedPass = escapeWifiField(password)
        return if (security == "nopass") {
            "WIFI:T:nopass;S:$escapedSsid;;"
        } else {
            "WIFI:T:$security;S:$escapedSsid;P:$escapedPass;;"
        }
    }

    private fun escapeWifiField(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace(":", "\\:")
    }
}
