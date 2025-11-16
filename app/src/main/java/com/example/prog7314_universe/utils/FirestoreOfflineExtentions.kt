package com.example.prog7314_universe.utils

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

private fun <T> Task<T>.withOfflineFallback(fallback: () -> Task<T>): Task<T> {
    return continueWithTask { task ->
        when {
            task.isSuccessful -> Tasks.forResult(task.result)
            task.exception is FirebaseFirestoreException &&
                    (task.exception as FirebaseFirestoreException).code == FirebaseFirestoreException.Code.UNAVAILABLE -> fallback()
            else -> Tasks.forException(task.exception ?: Exception("Unknown Firestore error"))
        }
    }
}

fun DocumentReference.getWithOfflineFallbackTask(): Task<DocumentSnapshot> {
    return get().withOfflineFallback { get(Source.CACHE) }
}

fun Query.getWithOfflineFallbackTask(): Task<QuerySnapshot> {
    return get().withOfflineFallback { get(Source.CACHE) }
}

suspend fun DocumentReference.getWithOfflineFallback(): DocumentSnapshot {
    return try {
        get().await()
    } catch (e: FirebaseFirestoreException) {
        if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
            get(Source.CACHE).await()
        } else {
            throw e
        }
    }
}

suspend fun Query.getWithOfflineFallback(): QuerySnapshot {
    return try {
        get().await()
    } catch (e: FirebaseFirestoreException) {
        if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
            get(Source.CACHE).await()
        } else {
            throw e
        }
    }
}