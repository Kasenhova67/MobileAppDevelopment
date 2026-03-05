package com.example.calculator.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calculator.Domain.CalculationResult
import com.example.calculator.Domain.CalculateExpressionUseCase
import com.example.calculator.Domain.ValidateExpressionUseCase
import kotlinx.coroutines.launch

class MainViewModel(
    private val calculateExpressionUseCase: CalculateExpressionUseCase,
    private val validateExpressionUseCase: ValidateExpressionUseCase
) : ViewModel() {

    private val _expression = MutableLiveData("")
    val expression: LiveData<String> = _expression

    private val _result = MutableLiveData("0")
    val result: LiveData<String> = _result

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _isResultShown = MutableLiveData(false)
    val isResultShown: LiveData<Boolean> = _isResultShown

    fun addCharacter(char: String) {

        if (_isResultShown.value == true) {
            _expression.value = ""
            _isResultShown.value = false
        }

        val currentExpression = _expression.value ?: ""

        if (char in listOf("+", "-", "*", "/")) {
            if (currentExpression.isEmpty()) {

                _error.value = "Expression cannot start with operator"
                return
            }
            val lastChar = currentExpression.last().toString()
            if (lastChar in listOf("+", "-", "*", "/")) {
                // Нельзя ставить два оператора подряд
                _error.value = "Cannot use two operators in a row"
                return
            }
        }

        if (char == ".") {
            val lastNumber = currentExpression.split(Regex("[+\\-*/]")).last()
            if (lastNumber.contains(".")) {
                _error.value = "Number already contains decimal point"
                return
            }
        }

        val newExpression = currentExpression + char

        if (validateExpressionUseCase(newExpression)) {
            _expression.value = newExpression
            _error.value = null
        } else {
            _error.value = "Invalid expression"
        }
    }

    fun removeLastCharacter() {
        val currentExpression = _expression.value ?: ""
        if (currentExpression.isNotEmpty()) {
            _expression.value = currentExpression.dropLast(1)
            _error.value = null
        }
    }

    fun calculate() {
        val expression = _expression.value ?: return

        if (expression.isEmpty()) {
            _error.value = "Enter expression"
            return
        }

        // Проверка на некорректное окончание
        val lastChar = expression.last().toString()
        if (lastChar in listOf("+", "-", "*", "/", ".")) {
            _error.value = "Expression cannot end with operator or dot"
            return
        }

        viewModelScope.launch {
            when (val result = calculateExpressionUseCase(expression)) {
                is CalculationResult.Success -> {
                    _result.value = result.value
                    _expression.value = result.value
                    _isResultShown.value = true
                    _error.value = null
                }
                is CalculationResult.Error -> {
                    _error.value = result.message
                    _result.value = "Error"
                }
                is CalculationResult.Empty -> {
                    _error.value = "Enter expression"
                }
            }
        }
    }

    fun clear() {
        _expression.value = ""
        _result.value = "0"
        _error.value = null
        _isResultShown.value = false
    }

    fun clearError() {
        _error.value = null
    }
}