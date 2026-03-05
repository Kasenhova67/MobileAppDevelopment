package com.example.calculator.Domain

sealed class CalculationResult {
    data class Success(val value: String) : CalculationResult()
    data class Error(val message: String) : CalculationResult()
    object Empty : CalculationResult()
}