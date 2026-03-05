package com.example.calculator.Domain
import com.example.calculator.Domain.CalculationResult
import com.example.calculator.Domain.CalculatorRepository

class CalculateExpressionUseCase(
    private val repository: CalculatorRepository
) {
    operator fun invoke(expression: String): CalculationResult {
        return if (expression.isEmpty()) {
            CalculationResult.Empty
        } else {
            repository.calculate(expression)
        }
    }
}