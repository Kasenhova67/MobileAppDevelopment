package com.example.calculator


import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {


    private lateinit var display: TextView
    private var expression = ""
    private var isResultShown = false

    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        display = findViewById(R.id.displayText)
    }

    fun onNumberClick(view: View) {

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

        val operator = (view as Button).text.toString()

        if (expression.isEmpty()) return
        isResultShown = false

        if (expression.last().toString() in listOf("+", "-", "*", "/"))
            return

        expression += operator
        display.text = expression
    }

    fun onEqualClick(view: View) {

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





}