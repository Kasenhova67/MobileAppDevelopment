package com.example.calculator.Data

import com.example.calculator.Domain.model.HistoryItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*

class HistoryRepository {

    private val db: FirebaseFirestore = Firebase.firestore
    private val collection = db.collection("history")

    suspend fun saveCalculation(expression: String, result: String) {
        val historyItem = HistoryItem(
            id = UUID.randomUUID().toString(),
            expression = expression,
            result = result,
            timestamp = Date()
        )
        collection.document(historyItem.id).set(historyItem).await()
    }

    fun loadHistory(): Flow<List<HistoryItem>> = callbackFlow {
        val listener = collection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(HistoryItem::class.java)
                } ?: emptyList()
                trySend(items).isSuccess
            }
        awaitClose { listener.remove() }
    }

    suspend fun clearHistory() {
        val documents = collection.get().await()
        for (doc in documents.documents) {
            doc.reference.delete().await()
        }
    }
}