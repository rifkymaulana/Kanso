package com.kanso.app.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.kanso.core.model.OcrReceiptLineMerger
import com.kanso.core.model.OcrTextLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlin.math.roundToInt

data class ProcessedSharedReceipt(
    val extractedText: String,
    val previewBitmap: ImageBitmap?
)

object ReceiptShareProcessor {

    private const val MAX_OCR_DIMENSION = 1800
    private const val MAX_PREVIEW_DIMENSION = 720
    private const val OCR_TIMEOUT_MS = 8_000L
    private const val MODULE_INSTALL_TIMEOUT_MS = 5_000L

    fun warmUpOcrModule(context: Context) {
        runCatching {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val request = ModuleInstallRequest.newBuilder()
                .addApi(recognizer)
                .build()
            ModuleInstall.getClient(context)
                .installModules(request)
                .addOnCompleteListener {
                    runCatching { recognizer.close() }
                }
        }
    }

    suspend fun processUri(
        context: Context,
        uri: Uri,
        declaredMimeType: String?
    ): ProcessedSharedReceipt = withContext(Dispatchers.IO) {
        if (isInternalAppFileUri(context, uri)) {
            return@withContext ProcessedSharedReceipt(extractedText = "", previewBitmap = null)
        }

        val resolvedMime = runCatching { context.contentResolver.getType(uri) }.getOrNull()
        val isPdf = declaredMimeType.equals("application/pdf", ignoreCase = true) ||
            resolvedMime.equals("application/pdf", ignoreCase = true) ||
            uri.lastPathSegment?.endsWith(".pdf", ignoreCase = true) == true

        val ocrBitmap = runCatching {
            if (isPdf) {
                decodePdfFirstPage(context, uri)
            } else {
                decodeImageBitmap(context, uri)
            }
        }.getOrNull() ?: return@withContext ProcessedSharedReceipt(extractedText = "", previewBitmap = null)

        val previewImageBitmap = createPreviewBitmap(ocrBitmap)?.asImageBitmap()
        val extractedText = recognizeReceiptText(context, ocrBitmap)

        ProcessedSharedReceipt(
            extractedText = extractedText,
            previewBitmap = previewImageBitmap
        )
    }

    private fun isInternalAppFileUri(context: Context, uri: Uri): Boolean {
        if (!uri.scheme.equals("file", ignoreCase = true)) return false
        val path = uri.path ?: return true
        return runCatching {
            val file = File(path).canonicalFile
            val dataDir = File(context.applicationInfo.dataDir).canonicalFile
            file.path == dataDir.path || file.path.startsWith(dataDir.path + File.separator)
        }.getOrDefault(true)
    }

    private fun decodePdfFirstPage(context: Context, uri: Uri): Bitmap? {
        return context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                if (renderer.pageCount <= 0) return null
                renderer.openPage(0).use { page ->
                    val pageWidth = page.width.coerceAtLeast(1)
                    val pageHeight = page.height.coerceAtLeast(1)
                    val maxDim = max(pageWidth, pageHeight).toFloat()
                    val scale = (MAX_OCR_DIMENSION.toFloat() / maxDim).coerceIn(0.5f, 2.5f)
                    val targetWidth = (pageWidth * scale).roundToInt().coerceAtLeast(1)
                    val targetHeight = (pageHeight * scale).roundToInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                    Canvas(bitmap).drawColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                }
            }
        }
    }

    private fun decodeImageBitmap(context: Context, uri: Uri): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val width = info.size.width.coerceAtLeast(1)
                val height = info.size.height.coerceAtLeast(1)
                val maxDim = max(width, height)
                if (maxDim > MAX_OCR_DIMENSION) {
                    val scale = MAX_OCR_DIMENSION.toFloat() / maxDim.toFloat()
                    decoder.setTargetSize(
                        (width * scale).roundToInt().coerceAtLeast(1),
                        (height * scale).roundToInt().coerceAtLeast(1)
                    )
                }
            }
        }

        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        }
        val rawWidth = boundsOptions.outWidth
        val rawHeight = boundsOptions.outHeight
        if (rawWidth <= 0 || rawHeight <= 0) return null

        var sampleSize = 1
        while (max(rawWidth / sampleSize, rawHeight / sampleSize) > MAX_OCR_DIMENSION) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        }
    }

    private fun createPreviewBitmap(source: Bitmap): Bitmap? {
        val width = source.width.coerceAtLeast(1)
        val height = source.height.coerceAtLeast(1)
        val maxDim = max(width, height)
        if (maxDim <= MAX_PREVIEW_DIMENSION) {
            return source.copy(Bitmap.Config.ARGB_8888, false)
        }
        val scale = MAX_PREVIEW_DIMENSION.toFloat() / maxDim.toFloat()
        val targetWidth = (width * scale).roundToInt().coerceAtLeast(1)
        val targetHeight = (height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }

    private suspend fun recognizeReceiptText(context: Context, bitmap: Bitmap): String {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val visionText = try {
                withTimeoutOrNull(OCR_TIMEOUT_MS) {
                    recognizer.process(image).awaitResult()
                }
            } catch (_: MlKitException) {
                val installed = withTimeoutOrNull(MODULE_INSTALL_TIMEOUT_MS) {
                    runCatching {
                        val request = ModuleInstallRequest.newBuilder()
                            .addApi(recognizer)
                            .build()
                        ModuleInstall.getClient(context).installModules(request).awaitResult()
                        true
                    }.getOrDefault(false)
                } ?: false
                if (installed) {
                    withTimeoutOrNull(OCR_TIMEOUT_MS) {
                        runCatching { recognizer.process(image).awaitResult() }.getOrNull()
                    }
                } else {
                    null
                }
            } ?: return ""

            val lines = mutableListOf<OcrTextLine>()
            for (block in visionText.textBlocks) {
                for (line in block.lines) {
                    val box = line.boundingBox
                    val trimmed = line.text.trim()
                    if (box != null && trimmed.isNotEmpty()) {
                        lines.add(
                            OcrTextLine(
                                text = trimmed,
                                left = box.left,
                                top = box.top,
                                right = box.right,
                                bottom = box.bottom
                            )
                        )
                    }
                }
            }
            if (lines.isNotEmpty()) {
                OcrReceiptLineMerger.mergeVisualRows(lines)
            } else {
                visionText.text.trim()
            }
        } catch (_: Throwable) {
            ""
        } finally {
            runCatching { recognizer.close() }
        }
    }

    private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result ->
            if (cont.isActive) cont.resume(result)
        }
        addOnFailureListener { error ->
            if (cont.isActive) cont.resumeWithException(error)
        }
        addOnCanceledListener {
            if (cont.isActive) cont.cancel()
        }
    }
}
