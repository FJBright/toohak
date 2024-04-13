package com.example.notKahoot.ui.viewModel

import android.app.Activity
import com.google.android.gms.common.api.Status
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.CoroutineScope



class NearbyConnectionModel(
    activity: Activity,
    coroutineScope: CoroutineScope,
    name: String
): NearbyConnectionWrapper(
    activity,
    coroutineScope,
    name,
    "TODO_SERVICE_ID",
    Strategy.P2P_STAR
) {
    var delegate: NearbyConnectionDelegate? = null

    override fun onAdvertisingStarted() {
        super.onAdvertisingStarted()
        delegate?.onAdvertisingStarted()
    }

    /** Called when advertising fails to start. Override this method to act on the event.  */
    override fun onAdvertisingFailed() {
        super.onAdvertisingFailed()
        delegate?.onAdvertisingFailed()
    }

    /**
     * Called when a pending connection with a remote endpoint is created. Use [ConnectionInfo]
     * for metadata about the connection (like incoming vs outgoing, or the authentication token). If
     * we want to continue with the connection, call [.acceptConnection]. Otherwise,
     * call [.rejectConnection].
     */
    override fun onConnectionInitiated(endpoint: Endpoint, connectionInfo: ConnectionInfo) {
        super.onConnectionInitiated(endpoint, connectionInfo)
        delegate?.onConnectionInitiated(endpoint, connectionInfo)
    }

    /** Called when discovery successfully starts. Override this method to act on the event.  */
    override fun onDiscoveryStarted() {
        super.onDiscoveryStarted()
        delegate?.onDiscoveryStarted()
    }

    /** Called when discovery fails to start. Override this method to act on the event.  */
    override fun onDiscoveryFailed(e: Exception) {
        super.onDiscoveryFailed(e)
        delegate?.onDiscoveryFailed(e)
    }

    /**
     * Called when a remote endpoint is discovered. To connect to the device, call [ ][.connectToEndpoint].
     */
    override fun onEndpointDiscovered(endpoint: Endpoint) {
        super.onEndpointDiscovered(endpoint)
        delegate?.onEndpointDiscovered(endpoint)
    }

    override fun onEndpointLost(endpoint: Endpoint) {
        super.onEndpointLost(endpoint)
        delegate?.onEndpointLost(endpoint)
    }

    /**
     * Called when a connection with this endpoint has failed. Override this method to act on the
     * event.
     */
    override fun onConnectionFailed(endpointId: String, exception: Exception) {
        super.onConnectionFailed(endpointId, exception)
        delegate?.onConnectionFailed(endpointId, exception)
    }

    /** Called when someone has connected to us. Override this method to act on the event.  */
    override fun onEndpointConnected(endpoint: Endpoint) {
        super.onEndpointConnected(endpoint)
        delegate?.onEndpointConnected(endpoint)
    }


    /** Called when someone has disconnected. Override this method to act on the event.  */
    override fun onEndpointDisconnected(endpoint: Endpoint) {
        super.onEndpointDisconnected(endpoint)
        delegate?.onEndpointDisconnected(endpoint)
    }

    override fun onReceive(endpoint: Endpoint, payload: Payload) {
        super.onReceive(endpoint, payload)
        delegate?.onReceive(endpoint, payload)
    }

    abstract class NearbyConnectionDelegate {
        open fun onAdvertisingStarted() {}

        /** Called when advertising fails to start. Override this method to act on the event.  */
        open fun onAdvertisingFailed() {}

        /**
         * Called when a pending connection with a remote endpoint is created. Use [ConnectionInfo]
         * for metadata about the connection (like incoming vs outgoing, or the authentication token). If
         * we want to continue with the connection, call [.acceptConnection]. Otherwise,
         * call [.rejectConnection].
         */
        open fun onConnectionInitiated(endpoint: Endpoint, connectionInfo: ConnectionInfo) {}

        /** Called when discovery successfully starts. Override this method to act on the event.  */
        open fun onDiscoveryStarted() {}

        /** Called when discovery fails to start. Override this method to act on the event.  */
        open fun onDiscoveryFailed(e: Exception) {}

        /**
         * Called when a remote endpoint is discovered. To connect to the device, call [ ][.connectToEndpoint].
         */
        open fun onEndpointDiscovered(endpoint: Endpoint) {}

        /**
         * Called when a remote endpoint is lost. Opposite of `onEndpointDiscovered`
         */
        open fun onEndpointLost(endpoint: Endpoint) {}

        /**
         * Called when a connection with this endpoint has failed. Override this method to act on the
         * event.
         */
        open fun onConnectionFailed(endpointId: String, exception: Exception) {}

        /** Called when someone has connected to us. Override this method to act on the event.  */
        open fun onEndpointConnected(endpoint: Endpoint) {}

        /** Called when someone has disconnected. Override this method to act on the event.  */
        open fun onEndpointDisconnected(endpoint: Endpoint) {}

        open fun onReceive(endpoint: Endpoint, payload: Payload) {}
    }
}