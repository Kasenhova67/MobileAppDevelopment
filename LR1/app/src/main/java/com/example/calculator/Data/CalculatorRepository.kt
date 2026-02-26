package com.example.calculator.Data

import com.example.calculator.Domain.CalculatorUseCase

class CalculatorRepository {

    private val calculatorUseCase = CalculatorUseCase()

    fun executeCalculation(a: Double, b: Double, operator: String): Double {
        return calculatorUseCase.calculate(a, b, operator)
    }
}