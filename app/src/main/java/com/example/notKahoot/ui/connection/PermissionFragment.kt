package com.example.notKahoot.ui.connection

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import com.example.notKahoot.R
import com.example.notKahoot.databinding.FragmentPermissionBinding
import com.example.notKahoot.ui.createGame.playGame.ActivityGameplayViewModel

/**
 * A simple [Fragment] subclass.
 * Use the [PermissionFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PermissionFragment : Fragment() {
    private val TAG = "PermissionFragment"
    private lateinit var textView: TextView

    private var _binding: FragmentPermissionBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentPermissionBinding.inflate(inflater, container, false)

        textView = binding.permissionStatusText

        context?.let {
            if (!hasPermissions(it, *REQUIRED_PERMISSIONS)) {
                textView.text = "Permissions required"
                requestPermissions()
            } else {
                Log.d(TAG, "Permission already granted")
                textView.text = "Permissions granted"
            }
        } ?: run {
            Log.d(TAG, "Context is null")
            textView.text = "Permissions cannot be retrieved"
        }

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        context?.let {
            if (hasPermissions(it, *REQUIRED_PERMISSIONS)) {
                permissionGranted()
            }
        }
    }

    private fun permissionGranted() {
        val destination = R.id.action_permissionFragment_to_connectClientSetName
        view?.findNavController()?.navigate(destination, null)
    }

    companion object {
        @JvmStatic
        val REQUEST_CODE_REQUIRED_PERMISSIONS = 1

        // https://github.com/android/connectivity-samples/blob/main/NearbyConnectionsWalkieTalkie/app/src/main/java/com/google/location/nearby/apps/walkietalkie/ConnectionsActivity.java
        /**
         * These permissions are required before connecting to Nearby Connections.
         */
        @JvmStatic
        var REQUIRED_PERMISSIONS: Array<String> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) else arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )


        /** @return True if the app was granted all the permissions. False otherwise.
         */
        @JvmStatic
        fun hasPermissions(context: Context, vararg permissions: String?): Boolean {
            for (permission in permissions) {
                if (ContextCompat.checkSelfPermission(context, permission!!)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.d("TODO", "Permission $permission has NOT been granted");
                    return false
                }
                Log.d("TODO", "Permission $permission has been granted");
            }
            return true
        }
    }

    private fun requestPermissions() {
        // https://developer.android.com/training/permissions/requesting
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { isGranted: Map<String, Boolean> ->
            Log.d(TAG, "Permission requests: $isGranted")
            if (isGranted.values.all { it }) {
                Log.d(TAG, "All permissions granted")
                textView.text = "Permissions granted"
                permissionGranted()
            } else {
                Log.d(TAG, "Not all permissions granted")
                textView.text = "Missing required permissions:\n" +
                        isGranted.filter { !it.value }.map { it.key }.joinToString("\n")
            }
        }
        requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
    }
}