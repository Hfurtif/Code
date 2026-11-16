package com.example.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

sealed class NetworkStatus {
    data class Connected(val isWifi: Boolean, val isMetered: Boolean) : NetworkStatus()
    object Disconnected : NetworkStatus()

    val isOnline: Boolean
        get() = this is Connected
}

/**
 * Service Layer responsible for real-time network connectivity monitoring
 * and validation of internet reachability across Wi-Fi and Cellular networks.
 */
class NetworkConnectivityService(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    companion object {
        private const val TAG = "NetworkConnService"
    }

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _networkStatus = MutableStateFlow<NetworkStatus>(determineInitialStatus())
    val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateStatus()
        }

        override fun onLost(network: Network) {
            updateStatus()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            updateStatus()
        }
    }

    init {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register network callback: ${e.message}")
        }
    }

    private fun determineInitialStatus(): NetworkStatus {
        val cm = connectivityManager ?: return NetworkStatus.Disconnected
        val activeNetwork = cm.activeNetwork ?: return NetworkStatus.Disconnected
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return NetworkStatus.Disconnected

        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        if (!hasInternet && !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            return NetworkStatus.Disconnected
        }

        val isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val isMetered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        return NetworkStatus.Connected(isWifi = isWifi, isMetered = isMetered)
    }

    fun updateStatus() {
        scope.launch {
            val current = determineInitialStatus()
            _networkStatus.value = current
        }
    }

    /**
     * Synchronous fast check for network availability.
     */
    fun isNetworkAvailable(): Boolean {
        val cm = connectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Validates genuine Internet reachability by testing DNS / TCP handshake
     * to avoid captive portals and false positive connections.
     */
    suspend fun checkInternetReachability(): Boolean = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) return@withContext false
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress("8.8.8.8", 53), 1500)
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}
