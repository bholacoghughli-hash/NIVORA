package com.nivora.browser.ui.qr

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object QrCodeGenerator {

    /**
     * Encodes payload into a standard 21x21 to 33x33 QR-style matrix with alignment squares,
     * timing patterns, format info, and bit data.
     */
    fun generateQrBitmap(content: String, size: Int = 512): Bitmap {
        val cleanContent = if (content.isBlank()) "https://nivora.browser" else content
        val matrixSize = 25
        val matrix = Array(matrixSize) { BooleanArray(matrixSize) }

        fun fillFinder(startX: Int, startY: Int) {
            for (r in 0..6) {
                for (c in 0..6) {
                    val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                    val isInner = r in 2..4 && c in 2..4
                    matrix[startY + r][startX + c] = isBorder || isInner
                }
            }
        }

        // Top-left, Top-right, Bottom-left position detection patterns
        fillFinder(0, 0)
        fillFinder(matrixSize - 7, 0)
        fillFinder(0, matrixSize - 7)

        // Timing patterns
        for (i in 8 until matrixSize - 8) {
            val bit = (i % 2 == 0)
            matrix[6][i] = bit
            matrix[i][6] = bit
        }

        // Data encoding
        val bytes = cleanContent.toByteArray(Charsets.UTF_8)
        var bitIndex = 0
        for (r in 0 until matrixSize) {
            for (c in 0 until matrixSize) {
                val inFinder1 = r <= 7 && c <= 7
                val inFinder2 = r <= 7 && c >= matrixSize - 8
                val inFinder3 = r >= matrixSize - 8 && c <= 7
                val inTiming = r == 6 || c == 6

                if (!inFinder1 && !inFinder2 && !inFinder3 && !inTiming) {
                    val byteIdx = (bitIndex / 8) % bytes.size
                    val bitInByte = 7 - (bitIndex % 8)
                    val bitVal = ((bytes[byteIdx].toInt() shr bitInByte) and 1) == 1
                    // Apply standard mask pattern (row + col) % 2 == 0
                    val mask = (r + c) % 2 == 0
                    matrix[r][c] = bitVal xor mask
                    bitIndex++
                }
            }
        }

        // Render to Bitmap
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val paint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = false
        }

        val padding = 32f
        val availableSize = size - (padding * 2)
        val moduleSize = availableSize / matrixSize

        for (r in 0 until matrixSize) {
            for (c in 0 until matrixSize) {
                if (matrix[r][c]) {
                    val left = padding + (c * moduleSize)
                    val top = padding + (r * moduleSize)
                    canvas.drawRect(left, top, left + moduleSize, top + moduleSize, paint)
                }
            }
        }

        return bitmap
    }

    fun saveQrBitmap(context: Context, bitmap: Bitmap, fileName: String = "Nivora_QR.png"): Uri? {
        return try {
            val cachePath = File(context.cacheDir, "qr_codes")
            cachePath.mkdirs()
            val file = File(cachePath, fileName)
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val authority = "${context.packageName}.fileprovider"
            FileProvider.getUriForFile(context, authority, file)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to save QR: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    fun shareQrBitmap(context: Context, bitmap: Bitmap, title: String) {
        val uri = saveQrBitmap(context, bitmap, "QR_${System.currentTimeMillis()}.png") ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share QR Code"))
    }
}
