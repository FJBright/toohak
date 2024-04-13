package com.example.notKahoot.ui.connection

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.app.NavUtils
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.example.notKahoot.R
import com.example.notKahoot.databinding.FragmentConnectServerBinding
import com.example.notKahoot.ui.createGame.playGame.ActivityGameplayViewModel
import com.example.notKahoot.ui.viewModel.NearbyConnectionModel
import com.example.notKahoot.ui.viewModel.NearbyConnectionWrapper
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
class ConnectServerFragment : Fragment() {
    private val TAG = "ConnectServerFragment"

    private lateinit var adapter: NearbyDeviceListViewAdapter

    private var _binding: FragmentConnectServerBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private val vm: ActivityGameplayViewModel by activityViewModels()

    private val model get() = vm.connection


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentConnectServerBinding.inflate(inflater, container, false)

        Log.d(TAG, "onCreateView called")

        model.delegate = delegate

        return binding.root
    }

    private fun onMainThread(task: () -> Unit) {
        lifecycleScope.launch { withContext(Dispatchers.Main) { task() } }
    }

    private val delegate = object : NearbyConnectionModel.NearbyConnectionDelegate() {
        override fun onConnectionInitiated(
            endpoint: NearbyConnectionWrapper.Endpoint,
            connectionInfo: ConnectionInfo
        ) {
            model.acceptConnection(endpoint)
            onMainThread {
                adapter.devices.add(endpoint)
                adapter.notifyItemInserted(adapter.devices.count() - 1)
            }
        }

        override fun onEndpointDisconnected(endpoint: NearbyConnectionWrapper.Endpoint) {
            onMainThread {
                val index = adapter.devices.indexOf(endpoint)
                if (index != -1) {
                    adapter.devices.removeAt(index)
                    adapter.notifyItemRemoved(index)
                }
            }
        }

        override fun onConnectionFailed(endpointId: String, exception: Exception) {
            adapter.devices.find { it.id == endpointId }?.let {
                onEndpointDisconnected(it)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView: RecyclerView = binding.clientDeviceList
        adapter = NearbyDeviceListViewAdapter(mutableListOf(), null)
        recyclerView.adapter = adapter

        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val singlePlayerEnabled = prefs.getBoolean("single_player", false)
        if (singlePlayerEnabled) {
            findNavController().navigate(R.id.action_connectServerFragment_to_FragmentGameplayQuiz)
        }

        binding.buttonConnectServerNext.setOnClickListener {
            model.stopAdvertising()
            // Hand off control of the fragment to the next thread
            model.delegate = null
            findNavController().navigate(R.id.action_connectServerFragment_to_FragmentGameplayQuiz)
        }
        binding.clientDeviceList.addItemDecoration(
            DividerItemDecoration(requireContext(),
                DividerItemDecoration.VERTICAL)
        )

        activity?.let {
            it.onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                // Go to the previous activity
                NavUtils.navigateUpFromSameTask(it)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        model.startAdvertising()

        adapter.devices.clear()
        adapter.devices.addAll(model.connectedEndpoints.value?.values ?: emptyList())
        adapter.notifyDataSetChanged()
    }

    override fun onPause() {
        super.onPause()
        model.stopAdvertising()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}