package com.example.calculator.Data

import com.example.calculator.Data.LocalDataSource
import com.example.calculator.Domain.CalculationResult
import com.example.calculator.Domain.CalculatorRepository

class CalculatorRepositoryImpl(
    private val localDataSource: LocalDataSource
) : CalculatorRepository {

    override fun calculate(expression: String): CalculationResult {
        return try {
            val result = evaluateExpression(expression)
            val formattedResult = formatResult(result)

            localDataSource.saveLastResult(formattedResult)
            localDataSource.saveToHistory(expression, formattedResult)

            CalculationResult.Success(formattedResult)
        } catch (e: ArithmeticException) {
            CalculationResult.Error("Division by zero")
        } catch (e: Exception) {
            CalculationResult.Error("Invalid expression")
        }
    }

    override fun validateExpression(expression: String): Boolean {
        if (expression.isEmpty()) return false

        if (expression.last().toString() in listOf("+", "-", "*", "/", ".")) {
            return false
        }

        val operators = listOf("+", "-", "*", "/")
        for (i in 0 until expression.length - 1) {
            val current = expression[i].toString()
            val next = expression[i + 1].toString()
            if (current in operators && next in operators) {
                return false
            }
        }

        val numbers = expression.split(*operators.toTypedArray())
        for (number in numbers) {
            if (number.count { it == '.' } > 1) {
                return false
            }
        }

        return true
    }

    override fun getLastResult(): String = localDataSource.getLastResult()

    override fun saveLastResult(result: String) {
        localDataSource.saveLastResult(result)
    }

    private fun evaluateExpression(expression: String): Double {

        val tokens = mutableListOf<String>()
        var currentNumber = StringBuilder()

        for (char in expression) {
            if (char.isDigit() || char == '.') {
                currentNumber.append(char)
            } else {
                if (currentNumber.isNotEmpty()) {
                    tokens.add(currentNumber.toString())
                    currentNumber = StringBuilder()
                }
                tokens.add(char.toString())
            }
        }
        if (currentNumber.isNotEmpty()) {
            tokens.add(currentNumber.toString())
        }

        var i = 0
        while (i < tokens.size) {
            when (tokens[i]) {
                "*", "/" -> {
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
                }
                else -> i++
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

    private fun formatResult(result: Double): String {
        return if (result % 1.0 == 0.0) {
            result.toInt().toString()
        } else {
            String.format("%.10f", result).trimEnd('0').trimEnd('.')
        }
    }
}