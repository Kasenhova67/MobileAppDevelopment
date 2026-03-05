package com.example.calculator.Data
import android.content.SharedPreferences
import androidx.core.content.edit

class LocalDataSource(
    private val sharedPreferences: SharedPreferences
) {
    private companion object {
        const val KEY_LAST_RESULT = "last_result"
        const val KEY_EXPRESSION_HISTORY = "expression_history"
    }

    fun saveLastResult(result: String) {
        sharedPreferences.edit {
            putString(KEY_LAST_RESULT, result)
        }
    }

    fun getLastResult(): String =
        sharedPreferences.getString(KEY_LAST_RESULT, "") ?: ""

    fun saveToHistory(expression: String, result: String) {
        val history = getHistory().toMutableList()
        history.add("$expression = $result")
        if (history.size > 10) history.removeAt(0)

        sharedPreferences.edit {
            putStringSet(KEY_EXPRESSION_HISTORY, history.toSet())
        }
    }

    fun getHistory(): Set<String> =
        sharedPreferences.getStringSet(KEY_EXPRESSION_HISTORY, emptySet()) ?: emptySet()
}