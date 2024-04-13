package com.example.notKahoot.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.example.notKahoot.R
import com.example.notKahoot.ui.createGame.playGame.ActivityGameplayQuiz
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.*

/**
* Home screen fragment
* */
class HomeFragment : Fragment() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_home, container, false)

        // GPS share button
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Create game button
        view.findViewById<Button>(R.id.createGame)?.setOnClickListener (
            Navigation.createNavigateOnClickListener(
                R.id.action_navigation_home_to_CreateGameFragment,
                null
            )
        )

        // Let's users join a (not in-progress) game
        view.findViewById<Button>(R.id.join_game_button)?.setOnClickListener {
            val intent = Intent(requireActivity(), ActivityGameplayQuiz::class.java)
            intent.putExtra(ActivityGameplayQuiz.EXTRA_BOOLEAN_IS_SERVER, false)
            startActivity(intent)
        }

        // Permissions
        val requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) {isGranted: Boolean ->
                if (isGranted) {
                    Log.i("Permission: ", "Granted")
                } else {
                    Log.i("Permission: ", "Denied")
                }
            }

        // Share location button
        view.findViewById<Button>(R.id.shareButton)?.setOnClickListener {
            // Checks to see if Fine and Coarse locations permissions are enabled by user
            if (ActivityCompat.checkSelfPermission(
                    requireActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    requireActivity(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            } else {
                // Get the phones last know location
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location : Location? ->
                        // Intent to share location to other apps
                        // I know this is ugly but could not find another way of doing it
                        val intent = Intent(Intent.ACTION_SEND)
                        intent.setType("text/plain")
                        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject))

                        // possible for location to be null thus changed th intent text in that case
                        if (location == null) {
                            intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_null_location))
                        } else {
                            // Geocoder takes in the latitude and longitude and gives an address
                            val geocoder = Geocoder(requireActivity(), Locale.getDefault())
                            val address = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                            intent.putExtra(Intent.EXTRA_TEXT,
                                "${getString(R.string.share_location_text)}" +
                                        "${address?.get(0)?.getAddressLine(0)}")
                        }
                        startActivity(Intent.createChooser(intent, getString(R.string.share_with)))
                    }
            }
        }

        return view
    }
}
