package com.example.calculator.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calculator.Domain.CalculationResult
import com.example.calculator.Data.ThemeRepository
import com.example.calculator.Domain.model.HistoryItem
import com.example.calculator.Data.HistoryRepository
import com.example.calculator.Domain.CalculateExpressionUseCase
import com.example.calculator.Domain.ValidateExpressionUseCase
import kotlinx.coroutines.launch

class MainViewModel(
    private val calculateExpressionUseCase: CalculateExpressionUseCase,
    private val validateExpressionUseCase: ValidateExpressionUseCase
) : ViewModel() {

    private val _themeColors = MutableLiveData<ThemeRepository.ThemeColors?>()
    val themeColors: LiveData<ThemeRepository.ThemeColors?> = _themeColors
    private val themeRepository = ThemeRepository()

    fun loadTheme() {
        viewModelScope.launch {
            try {
                themeRepository.initRemoteConfig()
                val colors = themeRepository.fetchThemeColors()
                _themeColors.value = colors
            } catch (e: Exception) {
                _error.value = "Failed to load theme: ${e.message}"
            }
        }
    }

    fun forceReloadTheme() {
        viewModelScope.launch {
            try {
                val colors = themeRepository.forceFetchThemeColors()
                _themeColors.value = colors
            } catch (e: Exception) {
                _error.value = "Failed to reload theme: ${e.message}"
            }
        }
    }

    private val _history = MutableLiveData<List<HistoryItem>>(emptyList())
    val history: LiveData<List<HistoryItem>> = _history
    private val historyRepository = HistoryRepository()

    fun loadHistory() {
        viewModelScope.launch {
            try {
                historyRepository.loadHistory().collect { items ->
                    _history.value = items
                }
            } catch (e: Exception) {
                _error.value = "Failed to load history: ${e.message}"
            }
        }
    }

    private fun saveToHistory(expression: String, result: String) {
        viewModelScope.launch {
            try {
                historyRepository.saveCalculation(expression, result)
            } catch (e: Exception) {
                _error.value = "Failed to save history: ${e.message}"
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            try {
                historyRepository.clearHistory()
                loadHistory()
            } catch (e: Exception) {
                _error.value = "Failed to clear history: ${e.message}"
            }
        }
    }

    private val _expression = MutableLiveData("")
    val expression: LiveData<String> = _expression

    private val _result = MutableLiveData("0")
    val result: LiveData<String> = _result

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _isResultShown = MutableLiveData(false)
    val isResultShown: LiveData<Boolean> = _isResultShown

    fun setExpression(expression: String) {
        _expression.value = expression
        _isResultShown.value = false
    }

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

        val lastChar = expression.last().toString()
        if (lastChar in listOf("+", "-", "*", "/", ".")) {
            _error.value = "Expression cannot end with operator or dot"
            return
        }

        when (val result = calculateExpressionUseCase(expression)) {
            is CalculationResult.Success -> {
                _result.value = result.value
                _expression.value = result.value
                _isResultShown.value = true
                _error.value = null
                saveToHistory(expression, result.value)
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