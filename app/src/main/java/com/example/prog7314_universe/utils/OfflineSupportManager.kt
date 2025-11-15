package com.example.prog7314_universe.utils

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object OfflineSupportManager {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    fun init(application: Application) {
        if (FirebaseApp.getApps(application).isEmpty()) {
            FirebaseApp.initializeApp(application)
        }

        configureFirestorePersistence()
        observeNetwork(application)
    }

    private fun configureFirestorePersistence() {
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        firestore.firestoreSettings = settings
    }

    private fun observeNetwork(application: Application) {
        val manager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return

        val initialConnected = manager.isCurrentlyConnected()
        updateOfflineState(!initialConnected)

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                updateOfflineState(false)
            }

            override fun onLost(network: Network) {
                val connected = manager.isCurrentlyConnected()
                updateOfflineState(!connected)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                manager.registerDefaultNetworkCallback(callback)
            } else {
                val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
                manager.registerNetworkCallback(request, callback)
            }
        } catch (_: Exception) {
            // Ignore failures, app will rely on cached data if we cannot observe network
        }
    }

    private fun updateOfflineState(isOffline: Boolean) {
        if (_isOffline.value == isOffline) return

        _isOffline.value = isOffline
        if (isOffline) {
            firestore.disableNetwork()
        } else {
            firestore.enableNetwork()
        }
    }

    private fun ConnectivityManager.isCurrentlyConnected(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = activeNetwork ?: return false
            val capabilities = getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            activeNetworkInfo?.isConnectedOrConnecting == true
        }
    }
}