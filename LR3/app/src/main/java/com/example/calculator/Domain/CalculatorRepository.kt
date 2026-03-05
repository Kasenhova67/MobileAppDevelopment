package com.example.calculator.Domain
import com.example.calculator.Domain.CalculationResult

interface CalculatorRepository {
    fun calculate(expression: String): CalculationResult
    fun validateExpression(expression: String): Boolean
    fun getLastResult(): String
    fun saveLastResult(result: String)
}