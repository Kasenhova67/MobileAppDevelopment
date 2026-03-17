package com.example.calculator.Data

import android.util.Log
import com.example.calculator.Domain.model.HistoryItem
import com.example.calculator.utils.NotificationManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*

class HistoryRepository(
    private val notificationManager: NotificationManager? = null
) {

    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("history")

    suspend fun saveCalculation(expression: String, result: String) {
        try {
            val historyItem = HistoryItem(
                id = UUID.randomUUID().toString(),
                expression = expression,
                result = result,
                timestamp = Date()
            )

            collection.document(historyItem.id).set(historyItem).await()

            saveToLocalHistory(historyItem)

            Log.d("HistoryRepository", "Saved: $expression = $result")

            notificationManager?.subscribeToTopic("calculations")

        } catch (e: Exception) {
            Log.e("HistoryRepository", "Error saving history", e)

            saveToLocalHistoryOffline(expression, result)
            throw e
        }
    }

    private fun saveToLocalHistory(historyItem: HistoryItem) {

    }

    private fun saveToLocalHistoryOffline(expression: String, result: String) {

    }

    fun loadHistory(): Flow<List<HistoryItem>> = callbackFlow {
        val listener = collection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {

                    trySend(loadLocalHistory()).isSuccess
                    return@addSnapshotListener
                }

                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(HistoryItem::class.java)
                } ?: emptyList()

                trySend(items).isSuccess
            }

        awaitClose { listener.remove() }
    }

    private fun loadLocalHistory(): List<HistoryItem> {

        return emptyList()
    }

    suspend fun clearHistory() {
        try {
            val documents = collection.get().await()
            for (doc in documents.documents) {
                doc.reference.delete().await()
            }


            sendHistoryClearedNotification()

        } catch (e: Exception) {
            Log.e("HistoryRepository", "Error clearing history", e)
        }
    }

    private suspend fun sendHistoryClearedNotification() {

    }
}