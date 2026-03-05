package com.example.calculator.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.calculator.Domain.CalculateExpressionUseCase
import com.example.calculator.Domain.ValidateExpressionUseCase

class MainViewModelFactory(
    private val calculateExpressionUseCase: CalculateExpressionUseCase,
    private val validateExpressionUseCase: ValidateExpressionUseCase
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(
                calculateExpressionUseCase,
                validateExpressionUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}