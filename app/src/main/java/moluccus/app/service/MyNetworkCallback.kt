package moluccus.app.service

import android.net.ConnectivityManager
import android.net.Network

class MyNetworkCallback : ConnectivityManager.NetworkCallback() {
    override fun onAvailable(network: Network) {
        super.onAvailable(network)
        // The network is available
    }

    override fun onLost(network: Network) {
        super.onLost(network)
        // The network is lost
    }
}
