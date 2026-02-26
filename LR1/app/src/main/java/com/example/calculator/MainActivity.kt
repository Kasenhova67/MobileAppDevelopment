package com.example.calculator.presentation

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.calculator.R

class MainActivity : AppCompatActivity() {

    private lateinit var display: TextView
    private var expression = ""
    private var isNewOperation = false
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        display = findViewById(R.id.displayText)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
    }

    fun onNumberClick(view: View) {
        val value = (view as Button).text.toString()

        if (isNewOperation) {
            expression = ""
            isNewOperation = false
        }

        if (value == ".") {
            val lastNumber = expression.split(Regex("[+\\-*/]")).last()
            if (lastNumber.contains(".")) return
        }

        expression += value
        display.text = expression
    }

    fun onOperatorClick(view: View) {
        val operator = (view as Button).text.toString()

        if (expression.isEmpty()) return

        if (isNewOperation) {
            isNewOperation = false
        }

        val lastChar = expression.last().toString()
        if (lastChar in listOf("+", "-", "*", "/")) {
            return
        }

        expression += operator
        display.text = expression
    }

    fun onEqualClick(view: View) {
        try {
            if (expression.isEmpty() || expression.last().toString() in listOf("+", "-", "*", "/")) {
                display.text = "Error"
                expression = ""
                return
            }

            val result = evaluateExpression(expression)

            val resultText = if (result == result.toInt().toDouble()) {
                result.toInt().toString()
            } else {
                result.toString()
            }

            display.text = resultText
            expression = resultText
            isNewOperation = true
        } catch (e: Exception) {
            display.text = "Error"
            expression = ""
            isNewOperation = false
        }
    }

    fun onClearClick(view: View) {
        expression = ""
        display.text = "0"
        isNewOperation = false
    }

    private fun evaluateExpression(expr: String): Double {

        val tokens = mutableListOf<String>()
        var number = ""

        for (char in expr) {
            if (char.isDigit() || char == '.') {
                number += char
            } else {
                if (number.isNotEmpty()) {
                    tokens.add(number)
                    number = ""
                }
                tokens.add(char.toString())
            }
        }
        if (number.isNotEmpty()) {
            tokens.add(number)
        }

        var i = 0
        while (i < tokens.size) {
            if (tokens[i] == "*" || tokens[i] == "/") {
                val left = tokens[i - 1].toDouble()
                val right = tokens[i + 1].toDouble()

                val result = if (tokens[i] == "*") {
                    left * right
                } else {
                    if (right == 0.0) throw ArithmeticException("Division by zero")
                    left / right
                }

                tokens[i - 1] = result.toString()
                tokens.removeAt(i)
                tokens.removeAt(i)

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