package com.example.notKahoot.ui.viewModel

import android.app.Activity
import android.util.Log
import androidx.annotation.CallSuper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.android.gms.tasks.OnFailureListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
A class that connects to Nearby Connections and provides convenience methods and callbacks.
From https://github.com/android/connectivity-samples/blob/main/NearbyConnectionsWalkieTalkie/app/src/main/java/com/google/location/nearby/apps/walkietalkie/ConnectionsActivity.java
Converted to Kotlin and has LiveData added.
*/
open class NearbyConnectionWrapper(
    activity: Activity,
    coroutineScope: CoroutineScope,
    name: String,
    serviceId: String,
    strategy: Strategy?
) {
    val TAG = "NearbyConnectionWrapper"

    /** Our handler to Nearby Connections.  */
    private var mConnectionsClient: ConnectionsClient

    /** Returns the client's name. Visible to others when connecting.  */
    var name: String

    /**
     * Returns the service id. This represents the action this connection is for. When discovering,
     * we'll verify that the advertiser has the same service id before we consider connecting to them.
     */
    protected val serviceId: String

    /**
     * Returns the strategy we use to connect to other devices. Only devices using the same strategy
     * and service id will appear when discovering. Strategies determine how many incoming and outgoing
     * connections are possible at the same time, as well as how much bandwidth is available for use.
     */
    protected val strategy: Strategy?

    protected val coroutineScope: CoroutineScope

    init {
        this.name = name
        this.serviceId = serviceId
        this.strategy = strategy
        this.mConnectionsClient = Nearby.getConnectionsClient(activity)
        this.coroutineScope = coroutineScope
    }

    protected fun <T> onMainThread(maybeNull: T?, callback: (T) -> Unit) {
        maybeNull?.let {
            coroutineScope.launch {
                withContext(Dispatchers.Main) {
                    callback(it)
                }
            }
        }
    }

    protected fun onMainThread(callback: () -> Unit) {
        coroutineScope.launch {
            withContext(Dispatchers.Main) {
                callback()
            }
        }
    }

    /** The devices we've discovered near us.  */
    private val mDiscoveredEndpoints = MutableLiveData<HashMap<String, Endpoint>>(HashMap())

    /**
     * The devices we have pending connections to. They will stay pending until we call [ ][.acceptConnection] or [.rejectConnection].
     */
    private val mPendingConnections = MutableLiveData<HashMap<String, Endpoint>>(HashMap())

    /**
     * The devices we are currently connected to. For advertisers, this may be large. For discoverers,
     * there will only be one entry in this map.
     */
    private val mEstablishedConnections = MutableLiveData<HashMap<String, Endpoint>>(HashMap())

    /** Returns `true` if we're currently attempting to connect to another device.  */
    /**
     * True if we are asking a discovered device to connect to us. While we ask, we cannot ask another
     * device.
     */
    private val mIsConnecting = MutableLiveData(false)
    val isConnecting: LiveData<Boolean> get() = mIsConnecting

    /** Returns `true` if currently discovering.  */
    /** True if we are discovering.  */
    private var mIsDiscovering = MutableLiveData(false)
    val isDiscovering: LiveData<Boolean> get() = mIsDiscovering

    /** Returns `true` if currently advertising.  */
    /** True if we are advertising.  */
    private var mIsAdvertising = MutableLiveData(false)
    val isAdvertising: LiveData<Boolean> get() = mIsAdvertising

    /** Callbacks for connections to other devices.  */
    private val mConnectionLifecycleCallback: ConnectionLifecycleCallback =
        object : ConnectionLifecycleCallback() {
            override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
                logD(
                    String.format(
                        "onConnectionInitiated(endpointId=%s, endpointName=%s)",
                        endpointId, connectionInfo.endpointName
                    )
                )
                val endpoint = Endpoint(endpointId, connectionInfo.endpointName)
                onMainThread {
                    mPendingConnections.value?.set(endpointId, endpoint)
                    mPendingConnections.value = mPendingConnections.value
                }
                this@NearbyConnectionWrapper.onConnectionInitiated(endpoint, connectionInfo)
            }

            override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                logD("onConnectionResponse(endpointId=$endpointId, result=$result)")

                // We're no longer connecting
                onMainThread {
                    mIsConnecting.value = false
                    mPendingConnections.value?.let {
                        val value = it.remove(endpointId)
                        mPendingConnections.value = it

                        if (!result.status.isSuccess) {
                            logW("Connection failed. Received status ${result.status}.")
                            onConnectionFailed(endpointId, Exception(result.status.statusMessage ?:
                            "Error connecting to endpoint $endpointId: ${result.status.statusCode}"))
                        } else {
                            value?.let {
                                connectedToEndpoint(it)
                            }
                        }
                    }
                }
            }

            override fun onDisconnected(endpointId: String) {
                mEstablishedConnections.value?.let {
                    if (it.containsKey(endpointId)) {
                        it[endpointId]?.let { endpoint ->
                            disconnectedFromEndpoint(endpoint)
                        }
                    } else {
                        logW("Unexpected disconnection from endpoint $endpointId")
                    }
                }
            }
        }

    /** Callbacks for payloads (bytes of data) sent from another device to us.  */
    private val mPayloadCallback: PayloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            logD("onPayloadReceived(endpointId=$endpointId, payload=$payload)")
            mEstablishedConnections.value?.get(endpointId)?.let {
                onReceive(it, payload)
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            logD("onPayloadTransferUpdate(endpointId=$endpointId, update=$update)")
        }
    }

    /**
     * Sets the device to advertising mode. It will broadcast to other devices in discovery mode.
     * Either [.onAdvertisingStarted] or [.onAdvertisingFailed] will be called once
     * we've found out if we successfully entered this mode.
     */
    fun startAdvertising() {
        onMainThread {
            mIsAdvertising.value = true
        }
        val localEndpointName = name
        val advertisingOptions = AdvertisingOptions.Builder()
        advertisingOptions.setStrategy(strategy!!)
        mConnectionsClient
            .startAdvertising(
                localEndpointName,
                serviceId,
                mConnectionLifecycleCallback,
                advertisingOptions.build()
            )
            .addOnSuccessListener {
                logV("Now advertising endpoint $localEndpointName")
                onAdvertisingStarted()
            }
            .addOnFailureListener(
                object : OnFailureListener {
                    override fun onFailure(e: Exception) {
                        onMainThread {
                            mIsAdvertising.value = false
                        }
                        logW("startAdvertising() failed.", e)
                        onAdvertisingFailed()
                    }
                })
    }

    /** Stops advertising.  */
    fun stopAdvertising() {
        onMainThread {
            mIsAdvertising.value = false
        }
        mConnectionsClient.stopAdvertising()
    }

    /** Called when advertising successfully starts. Override this method to act on the event.  */
    protected open fun onAdvertisingStarted() {}

    /** Called when advertising fails to start. Override this method to act on the event.  */
    protected open fun onAdvertisingFailed() {}

    /**
     * Called when a pending connection with a remote endpoint is created. Use [ConnectionInfo]
     * for metadata about the connection (like incoming vs outgoing, or the authentication token). If
     * we want to continue with the connection, call [.acceptConnection]. Otherwise,
     * call [.rejectConnection].
     */
    protected open fun onConnectionInitiated(endpoint: Endpoint, connectionInfo: ConnectionInfo) {}

    /** Accepts a connection request.  */
    fun acceptConnection(endpoint: Endpoint) {
        mConnectionsClient
            .acceptConnection(endpoint.id, mPayloadCallback)
            .addOnFailureListener { e -> logW("acceptConnection() failed.", e) }
    }

    /** Rejects a connection request.  */
    fun rejectConnection(endpoint: Endpoint) {
        mConnectionsClient
            .rejectConnection(endpoint.id)
            .addOnFailureListener { e -> logW("rejectConnection() failed.", e) }
    }

    /**
     * Sets the device to discovery mode. It will now listen for devices in advertising mode. Either
     * [.onDiscoveryStarted] or [.onDiscoveryFailed] will be called once we've found
     * out if we successfully entered this mode.
     */
    fun startDiscovering() {
        onMainThread {
            mIsAdvertising.value = true
            mIsDiscovering.value = true
            mDiscoveredEndpoints.value?.clear()
            mDiscoveredEndpoints.value = mDiscoveredEndpoints.value
        }

        val discoveryOptions = DiscoveryOptions.Builder()
        discoveryOptions.setStrategy(strategy!!)
        mConnectionsClient
            .startDiscovery(
                serviceId,
                object : EndpointDiscoveryCallback() {
                    override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                        logD(
                            String.format(
                                "onEndpointFound(endpointId=%s, serviceId=%s, endpointName=%s)",
                                endpointId, info.serviceId, info.endpointName
                            )
                        )
                        if (serviceId != info.serviceId) return;

                        if (mPendingConnections.value?.get(endpointId) != null ||
                            mEstablishedConnections.value?.get(endpointId) != null
                        ) {
                            // Already discovered
                            logD(
                                "Discovered endpoint $endpointId present in:\n" +
                                "  pending connections: ${mPendingConnections.value?.get(endpointId) != null}\n" +
                                "  established connections: ${mEstablishedConnections.value?.get(endpointId) != null}"
                            )
                            return
                        }

                        val endpoint = Endpoint(endpointId, info.endpointName)
                        onMainThread {
                            mDiscoveredEndpoints.value?.set(endpointId, endpoint)
                            mDiscoveredEndpoints.value = mDiscoveredEndpoints.value
                        }
                        onEndpointDiscovered(endpoint)
                    }

                    override fun onEndpointLost(endpointId: String) {
                        onMainThread {
                            mDiscoveredEndpoints.value?.remove(endpointId)?.let {
                                this@NearbyConnectionWrapper.onEndpointLost(it)
                            }
                        }
                        logD("onEndpointLost(endpointId=$endpointId)")
                    }
                },
                discoveryOptions.build()
            )
            .addOnSuccessListener { onDiscoveryStarted() }
            .addOnFailureListener { e ->
                onMainThread {
                    mIsDiscovering.value = false
                }
                logW("startDiscovering() failed.", e)
                onDiscoveryFailed(e)
            }
    }

    /** Stops discovery.  */
    fun stopDiscovering() {
        onMainThread {
            mIsDiscovering.value = false
        }
        mConnectionsClient.stopDiscovery()
    }

    /** Called when discovery successfully starts. Override this method to act on the event.  */
    protected open fun onDiscoveryStarted() {}

    /** Called when discovery fails to start. Override this method to act on the event.  */
    protected open fun onDiscoveryFailed(e: Exception) {}

    /**
     * Called when a remote endpoint is discovered. To connect to the device, call [ ][.connectToEndpoint].
     */
    protected open fun onEndpointDiscovered(endpoint: Endpoint) {}

    /**
     * Called when a remote endpoint is lost. Opposite of `onEndpointDiscovered`
     */
    protected open fun onEndpointLost(endpoint: Endpoint) {}

    /** Disconnects from the given endpoint.  */
    fun disconnect(endpoint: Endpoint) {
        mConnectionsClient?.disconnectFromEndpoint(endpoint.id)
        onMainThread {
            mEstablishedConnections.value?.remove(endpoint.id)
            mEstablishedConnections.value = mEstablishedConnections.value
        }
    }

    /** Disconnects from all currently connected endpoints.  */
    fun disconnectFromAllEndpoints() {
        logD("Disconnect from all endpoints")
        mEstablishedConnections.value?.let {
            for (endpoint in it.values) {
                mConnectionsClient.disconnectFromEndpoint(endpoint.id)
            }
            onMainThread {
                mEstablishedConnections.value?.clear()
                mEstablishedConnections.value = mEstablishedConnections.value
            }
        }
    }

    /** Resets and clears all state in Nearby Connections.  */
    fun stopAllEndpoints() {
        logD("Clear state and stop all endpoints")
        mConnectionsClient.stopAllEndpoints()
        onMainThread {
            mIsAdvertising.value = false
            mIsDiscovering.value = false
            mIsConnecting.value = false
            mDiscoveredEndpoints.value?.clear()
            mDiscoveredEndpoints.value = mDiscoveredEndpoints.value
            mPendingConnections.value?.clear()
            mPendingConnections.value = mPendingConnections.value
            mEstablishedConnections.value?.clear()
            mEstablishedConnections.value = mEstablishedConnections.value
        }
    }

    /**
     * Sends a connection request to the endpoint. Either [.onConnectionInitiated] or [.onConnectionFailed] will be called once we've found out
     * if we successfully reached the device.
     */
    fun connectToEndpoint(endpoint: Endpoint) {
        logV("Sending a connection request to endpoint $endpoint")
        // Mark ourselves as connecting so we don't connect multiple times
        onMainThread {
            mIsConnecting.value = true
        }

        // Ask to connect
        mConnectionsClient
            .requestConnection(name, endpoint.id, mConnectionLifecycleCallback)
            .addOnFailureListener(
                object : OnFailureListener {
                    override fun onFailure(e: Exception) {
                        logW("requestConnection() failed.", e)
                        onMainThread {
                            mIsConnecting.value = false
                            mPendingConnections.value?.remove(endpoint.id)
                            mPendingConnections.value = mPendingConnections.value
                        }
                        if (e is ApiException && e.statusCode == ConnectionsStatusCodes.STATUS_ALREADY_CONNECTED_TO_ENDPOINT) {
                            if (mEstablishedConnections.value?.containsKey(endpoint.id) == false) {
                                logD("Somehow lost track: already connected to endpoint ${endpoint} but not in mEstablishedConnections")
                                onMainThread {
                                    mEstablishedConnections.value?.set(endpoint.id, endpoint)
                                    mEstablishedConnections.value = mEstablishedConnections.value
                                }
                                onEndpointConnected(endpoint)
                            }
                            return
                        }
                        onConnectionFailed(endpoint.id, e)
                    }
                })
    }

    private fun connectedToEndpoint(endpoint: Endpoint) {
        logD("connectedToEndpoint(endpoint=$endpoint)")
        onMainThread(mEstablishedConnections.value) {
            it[endpoint.id] = endpoint
            mEstablishedConnections.value = mEstablishedConnections.value
            mPendingConnections.value?.remove(endpoint.id)
            mPendingConnections.value = mPendingConnections.value
            mIsConnecting.value = false
        }
        onEndpointConnected(endpoint)
    }

    private fun disconnectedFromEndpoint(endpoint: Endpoint) {
        logD("disconnectedFromEndpoint(endpoint=$endpoint)")
        onMainThread(mEstablishedConnections.value) {
            it.remove(endpoint.id)
            mEstablishedConnections.value = mEstablishedConnections.value
        }
        onEndpointDisconnected(endpoint)
    }

    /**
     * Called when a connection with this endpoint has failed. Override this method to act on the
     * event.
     */
    protected open fun onConnectionFailed(endpointId: String, exception: Exception) {}

    /** Called when someone has connected to us. Override this method to act on the event.  */
    protected open fun onEndpointConnected(endpoint: Endpoint) {}

    /** Called when someone has disconnected. Override this method to act on the event.  */
    protected open fun onEndpointDisconnected(endpoint: Endpoint) {}

    /** Returns a list of currently connected endpoints.  */
    val discoveredEndpoints: LiveData<HashMap<String, Endpoint>> get() = mDiscoveredEndpoints

    /** Returns a list of currently connected endpoints.  */
    val connectedEndpoints: LiveData<HashMap<String, Endpoint>> get() = mEstablishedConnections

    /**
     * Sends a [Payload] to all currently connected endpoints.
     *
     * @param payload The data you want to send.
     */
    fun send(payload: Payload) {
        val receiverIds = mEstablishedConnections.value?.keys ?: setOf()
        if (receiverIds.isEmpty()) {
            Log.d(TAG, "Did not send message: no receivers to send payload to")
            return
        }

        send(payload, receiverIds)
    }

    private fun send(payload: Payload, endpoints: Set<String>) {
        mConnectionsClient
            .sendPayload(ArrayList(endpoints), payload)
            .addOnFailureListener { e -> logW("sendPayload() failed.", e) }
    }

    /**
     * Someone connected to us has sent us data. Override this method to act on the event.
     *
     * @param endpoint The sender.
     * @param payload The data.
     */
    protected open fun onReceive(endpoint: Endpoint, payload: Payload) {}

    @CallSuper
    protected fun logV(msg: String?) {
        Log.v(TAG, msg!!)
    }

    @CallSuper
    protected fun logD(msg: String?) {
        Log.d(TAG, msg!!)
    }

    @CallSuper
    protected fun logW(msg: String?) {
        Log.w(TAG, msg!!)
    }

    @CallSuper
    protected fun logW(msg: String?, e: Throwable?) {
        Log.w(TAG, msg, e)
    }

    @CallSuper
    protected fun logE(msg: String?, e: Throwable?) {
        Log.e(TAG, msg, e)
    }

    /** Represents a device we can talk to.  */
    class Endpoint constructor(val id: String, val name: String) {
        override fun equals(obj: Any?): Boolean {
            if (obj is Endpoint) {
                return id == obj.id
            }
            return false
        }

        override fun hashCode(): Int {
            return id.hashCode()
        }

        override fun toString(): String {
            return "Endpoint{id=$id, name=$name}"
        }
    }
}