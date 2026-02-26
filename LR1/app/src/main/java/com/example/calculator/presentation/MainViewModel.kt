package com.example.calculator.presentation

import androidx.lifecycle.ViewModel
import com.example.calculator.Data.CalculatorRepository

class MainViewModel : ViewModel() {

    private val repository = CalculatorRepository()

    fun calculate(a: String, b: String, operator: String): String {
        return try {
            val first = a.toDouble()
            val second = b.toDouble()
            val result = repository.executeCalculation(first, second, operator)
            result.toString()
        } catch (e: Exception) {
            "Error"
        }
    }
}