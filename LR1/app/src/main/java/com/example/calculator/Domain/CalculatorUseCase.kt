package com.example.calculator.Domain

class CalculatorUseCase {

    fun calculate(first: Double, second: Double, operator: String): Double {
        return when (operator) {
            "+" -> first + second
            "-" -> first - second
            "*" -> first * second
            "/" -> {
                if (second == 0.0) throw ArithmeticException("Division by zero")
                first / second
            }
            else -> throw IllegalArgumentException("Unknown operator")
        }
    }
}