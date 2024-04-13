package com.example.notKahoot

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.app.NavUtils
import androidx.core.content.edit
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.navigation.findNavController
import androidx.preference.PreferenceManager
import com.example.notKahoot.databinding.FragmentConnectClientSetNameBinding
import com.example.notKahoot.ui.createGame.playGame.ActivityGameplayViewModel

/**
 * A simple [Fragment] subclass.
 * Use the [ConnectClientSetNameFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ConnectClientSetNameFragment : Fragment() {
    private var _binding: FragmentConnectClientSetNameBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private val activityVm: ActivityGameplayViewModel by activityViewModels()

    // Text entry value
    class VM: ViewModel() {
        var name: String = ""
    }
    private val vm: VM by viewModels()

    companion object {
        const val preferenceKeyMostRecentUserName = "PREFERENCE_KEY_MOST_RECENT_USER_NAME"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConnectClientSetNameBinding.inflate(inflater, container, false)

        binding.clientUserNameTextInput.addTextChangedListener {
            if (it == null) return@addTextChangedListener
            vm.name = it.trim().toString()
            binding.clientUserNameNextButton.isEnabled = vm.name.isNotEmpty()
        }

        binding.clientUserNameNextButton.setOnClickListener {
            activityVm.name = vm.name

            val context = requireContext()
            val unknownName = context.getString(R.string.unknown_name)
            val unknownQuizName = context.getString(R.string.unknown_quiz_name)
            activityVm.connection.name = if (activityVm.isServer)
                context.getString(R.string.server_advertised_name, activityVm.quizName ?: unknownName, activityVm.name ?: unknownQuizName)
                else activityVm.name ?: unknownName

            // Navigates the game host to the connect server fragment, client fragment otherwise
            val destination = if (activityVm.isServer)
                R.id.action_permissionFragment_to_connectServerFragment else
                R.id.action_connectClientSetName_to_connectClientFragment
            view?.findNavController()?.navigate(destination)

            activity?.getPreferences(Context.MODE_PRIVATE)?.edit(commit = true) {
                putString(preferenceKeyMostRecentUserName, vm.name)
            }
        }

        activity?.getPreferences(Context.MODE_PRIVATE)?.let {
            vm.name = it.getString(preferenceKeyMostRecentUserName, "") ?: ""
        }

        activity?.let {
            it.onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                // Go to the previous activity
                NavUtils.navigateUpFromSameTask(it)
            }
        }


        return binding.root
    }

    override fun onResume() {
        super.onResume()

        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val skipNameEnabled = prefs.getBoolean("skip_name", false)

        val unknownName = requireContext().getString(R.string.unknown_name)
        if (skipNameEnabled && vm.name.isNotEmpty() && vm.name != unknownName) {
            activityVm.name = vm.name
            // Navigates the game host to the connect server fragment, client fragment otherwise
            val destination = if (activityVm.isServer)
                R.id.action_permissionFragment_to_connectServerFragment else
                R.id.action_connectClientSetName_to_connectClientFragment
            view?.findNavController()?.navigate(destination)
        }

        binding.clientUserNameTextInput.setText(vm.name)
        binding.clientUserNameNextButton.isEnabled = vm.name.isNotEmpty()
    }

    override fun onPause() {
        super.onPause()
    }
}