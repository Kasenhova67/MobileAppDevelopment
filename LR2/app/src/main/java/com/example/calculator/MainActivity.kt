package com.example.calculator

import com.example.calculator.R
import android.media.SoundPool
import android.media.AudioAttributes
import android.content.Context

import android.graphics.Color
import android.graphics.Paint


import android.graphics.Bitmap
import android.graphics.Canvas
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var soundPool: SoundPool
    private var soundId: Int = 0

    private lateinit var display: TextView
    private var expression = ""
    private var isResultShown = false

    override fun onCreate(savedInstanceState: Bundle?) {

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(audioAttributes)
            .build()

        soundId = soundPool.load(this, R.raw.click, 1)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        display = findViewById(R.id.displayText)
    }

    fun onNumberClick(view: View) {
        playClickSound()
        val value = (view as Button).text.toString()

        if (isResultShown) {
            expression = ""
            isResultShown = false
        }

        if (value == ".") {
            val lastNumber = expression.split("+", "-", "*", "/").last()
            if (lastNumber.contains(".")) return
        }

        expression += value
        display.text = expression
    }

    fun onOperatorClick(view: View) {
        playClickSound()
        val operator = (view as Button).text.toString()

        if (expression.isEmpty()) return
        isResultShown = false

        if (expression.last().toString() in listOf("+", "-", "*", "/"))
            return

        expression += operator
        display.text = expression
    }

    fun onEqualClick(view: View) {
        playClickSound()
        try {
            val result = evaluateExpression(expression)

            val formatted = if (result % 1.0 == 0.0) {
                result.toInt().toString()
            } else {
                result.toString()
            }

            display.text = formatted
            expression = formatted
            isResultShown = true

        } catch (e: Exception) {
            display.text = "Error"
            expression = ""
        }
    }

    fun onClearClick(view: View) {
        playClickSound()
        expression = ""
        isResultShown = false
        display.text = "0"
    }
    private fun evaluateExpression(expr: String): Double {

        val tokens = mutableListOf<String>()
        var number = ""

        for (char in expr) {
            if (char.isDigit() || char == '.') {
                number += char
            } else {
                tokens.add(number)
                tokens.add(char.toString())
                number = ""
            }
        }
        tokens.add(number)

        var i = 0
        while (i < tokens.size) {
            if (tokens[i] == "*" || tokens[i] == "/") {
                val left = tokens[i - 1].toDouble()
                val right = tokens[i + 1].toDouble()
                val result = if (tokens[i] == "*") left * right else left / right

                tokens[i - 1] = result.toString()
                tokens.removeAt(i)
                tokens.removeAt(i)
                i--
            } else {
                i++
            }
        }

        var result = tokens[0].toDouble()
        i = 1
        while (i < tokens.size) {
            val operator = tokens[i]
            val nextNumber = tokens[i + 1].toDouble()

            result = if (operator == "+") {
                result + nextNumber
            } else {
                result - nextNumber
            }

            i += 2
        }

        return result
    }

    private fun playClickSound() {
        soundPool.play(soundId, 1f, 1f, 0, 0, 1f)

    }

    fun onShareClick(view: View?) {

        playClickSound()

        val display = findViewById<TextView>(R.id.displayText)
        val resultText = display.text.toString()

        val bitmap = Bitmap.createBitmap(800, 400, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawColor(Color.WHITE)

        val paint = Paint()
        paint.color = Color.BLACK
        paint.textSize = 80f
        paint.isAntiAlias = true

        canvas.drawText(resultText, 100f, 200f, paint)

        try {
            val file = File(cacheDir, "result.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "image/png"
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            startActivity(Intent.createChooser(intent, "Share result"))

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}