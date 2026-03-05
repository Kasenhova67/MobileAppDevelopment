package com.example.calculator.Domain

class ValidateExpressionUseCase(
    private val repository: CalculatorRepository
) {
    operator fun invoke(expression: String): Boolean {

        if (expression.isEmpty()) return true

        val lastChar = expression.last()

        return true
    }
}