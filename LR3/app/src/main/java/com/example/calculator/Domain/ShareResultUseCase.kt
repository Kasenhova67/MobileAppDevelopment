package com.example.calculator.Domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

class ShareResultUseCase {

    fun createResultImage(context: Context, result: String): File? {
        return try {
            val bitmap = Bitmap.createBitmap(800, 400, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            canvas.drawColor(Color.WHITE)

            val paint = Paint().apply {
                color = Color.BLACK
                textSize = 80f
                isAntiAlias = true
            }

            canvas.drawText(result, 100f, 200f, paint)

            val file = File(context.cacheDir, "result_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }

            file
        } catch (e: Exception) {
            null
        }
    }

    fun getUriForFile(context: Context, file: File) =
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
}