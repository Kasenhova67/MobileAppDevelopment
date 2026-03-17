package com.example.calculator.Domain.model

import java.util.Date

data class HistoryItem(
    val id: String = "",
    val expression: String = "",
    val result: String = "",
    val timestamp: Date = Date()
)