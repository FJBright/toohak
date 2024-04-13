package com.example.notKahoot.ui.connection

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.example.notKahoot.MainActivity
import com.example.notKahoot.R
import com.example.notKahoot.databinding.FragmentConnectClientBinding
import com.example.notKahoot.databinding.FragmentConnectServerBinding
import com.example.notKahoot.ui.createGame.playGame.ActivityGameplayQuiz
import com.example.notKahoot.ui.createGame.playGame.ActivityGameplayViewModel
import com.example.notKahoot.ui.helper.animateView
import com.example.notKahoot.ui.viewModel.NearbyConnectionModel
import com.example.notKahoot.ui.viewModel.NearbyConnectionWrapper
import com.example.notKahoot.utilities.NOTIFICATION_CHANNEL_ID
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class ConnectClientFragment : Fragment() {
    private val TAG = "ConnectClientFragment"

    private lateinit var adapter: NearbyDeviceListViewAdapter

    private var _binding: FragmentConnectClientBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private val vm: ActivityGameplayViewModel by activityViewModels()

    private val model get() = vm.connection

    private val connectedOrConnecting: Boolean get() {
        return model.isConnecting.value == true || model.connectedEndpoints.value?.isNotEmpty() == true
    }

    private fun updateLoadingOverlay(show: Boolean? = null) {
        animateView(
            binding.connectingToServerOverlay.progressOverlay,
            toVisibility = if (show ?: connectedOrConnecting) View.VISIBLE else View.INVISIBLE,
            toAlpha = 0.4f,
            bringToFront = true
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConnectClientBinding.inflate(inflater, container, false)

        // Needed for creating a notification
        createNotificationChannel()

//        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner) {
//            model.delegate = null
//            model.stopAllEndpoints(From the bad bad days of the )
//        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            model.stopAllEndpoints()
            model.stopDiscovering()
            model.startDiscovering()
            adapter.devices.clear()
            adapter.notifyDataSetChanged()

//            val id = UUID.randomUUID().toString().substring(0, 4)
//            delegate.onEndpointDiscovered(NearbyConnectionWrapper.Endpoint(id, "$id"))
            binding.swipeRefreshLayout.isRefreshing = false
        }

        binding.serverDeviceList.addItemDecoration(
            DividerItemDecoration(requireContext(),
            DividerItemDecoration.VERTICAL)
        )

        model.delegate = delegate

        return binding.root
    }

    private fun onMainThread(task: () -> Unit) {
        lifecycleScope.launch { withContext(Dispatchers.Main) { task() } }
    }

    private val delegate = object : NearbyConnectionModel.NearbyConnectionDelegate() {
        override fun onEndpointDiscovered(endpoint: NearbyConnectionWrapper.Endpoint) {
            onMainThread {
                adapter.devices.add(endpoint)
                adapter.notifyItemInserted(adapter.devices.count() - 1)
            }
        }

        override fun onEndpointLost(endpoint: NearbyConnectionWrapper.Endpoint) {
            onMainThread {
                val index = adapter.devices.indexOfFirst { it.id == endpoint.id }
                if (index != -1) {
                    adapter.devices.removeAt(index)
                    adapter.notifyItemRemoved(index)
                }
            }
        }

        override fun onDiscoveryFailed(e: Exception) {
            val context = context ?: return
            if (e !is ApiException) return;
            if (e.statusCode != ConnectionsStatusCodes.MISSING_PERMISSION_ACCESS_COARSE_LOCATION) return;
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                onMainThread {
                    Toast.makeText(context, "Please enable location on your phone", Toast.LENGTH_LONG).show()
                }
            }
        }

        override fun onConnectionFailed(endpointId: String, exception: Exception) {
            onMainThread {
                val index = adapter.devices.indexOfFirst { it.id == endpointId }
                if (index != -1) {
                    adapter.devices.removeAt(index)
                    adapter.notifyItemRemoved(index)
                }
                Toast.makeText(context, exception.message, Toast.LENGTH_LONG).show()
                updateLoadingOverlay(false)
            }
        }

        override fun onConnectionInitiated(
            endpoint: NearbyConnectionWrapper.Endpoint,
            connectionInfo: ConnectionInfo
        ) {
            model.acceptConnection(endpoint)
            // For whatever reason, the client needs to accept the connection, even if it was the one
            // that initiated the connection
            onMainThread {
                updateLoadingOverlay(false)
//                Toast.makeText(context, "CONNECTED TO ENDPOINT ${connectionInfo.endpointName}", Toast.LENGTH_LONG).show()
                connectedNotification()
                findNavController().navigate(R.id.action_connectClientFragment_to_FragmentGameplayQuiz)
            }
        }
    }

    private fun onEndpointClick(endpoint: NearbyConnectionWrapper.Endpoint) {
        if (connectedOrConnecting) return
        updateLoadingOverlay(true)
        model.connectToEndpoint(endpoint)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView: RecyclerView = binding.serverDeviceList
        adapter = NearbyDeviceListViewAdapter(mutableListOf(), ::onEndpointClick)
        recyclerView.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        model.startDiscovering()

        updateLoadingOverlay()
        adapter.devices.clear()
        adapter.devices.addAll(model.discoveredEndpoints.value?.values ?: emptyList())
        adapter.notifyDataSetChanged()
    }

    override fun onPause() {
        super.onPause()
        model.stopDiscovering()
    }

    override fun onStop() {
        super.onStop()
    }

    private fun connectedNotification() {
        val builder = NotificationCompat.Builder(requireActivity(), NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(requireActivity())) {
            // notificationId is a unique int for each notification that you must define
            notify(0, builder.build())
        }
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}