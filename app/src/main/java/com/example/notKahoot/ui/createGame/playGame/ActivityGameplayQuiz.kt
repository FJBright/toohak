package com.example.notKahoot.ui.createGame.playGame

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.notKahoot.MainActivity
import com.example.notKahoot.R
import com.example.notKahoot.databinding.ActivityGamplayBinding
import com.example.notKahoot.ui.viewModel.NearbyConnectionModel
import com.example.notKahoot.viewModel.QuestionViewModel
import com.example.notKahoot.viewModel.QuizViewModel
import java.util.UUID
import kotlin.properties.Delegates

class ActivityGameplayViewModel: ViewModel() {
    var isServer by Delegates.notNull<Boolean>()
    lateinit var connection: NearbyConnectionModel
    var uuid: UUID = UUID.randomUUID()
    var quizId: Int = -1
    var quizResults: GameplayQuizFragment.QuizResultsPayload? = null
    var name: String? = null
    var quizName: String? = null
}

class ActivityGameplayQuiz : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityGamplayBinding
    private var backButtonDisabled: Boolean = true

    private lateinit var mQuizViewModel: QuizViewModel
    private lateinit var mQuestionViewModel: QuestionViewModel
    val vm: ActivityGameplayViewModel by viewModels()

    companion object {
        val EXTRA_INT_QUIZ_ID = "quizId"
        val EXTRA_BOOLEAN_IS_SERVER = "isServer"
        val EXTRA_STRING_DEVICE_NAME = "name"
        val EXTRA_STRING_QUIZ_NAME = "quizName"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGamplayBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // QuizViewModel
        mQuizViewModel = ViewModelProvider(this)[QuizViewModel::class.java]
        mQuestionViewModel = ViewModelProvider(this)[QuestionViewModel::class.java]

        if (savedInstanceState == null) {
            val context = baseContext
            // Got here from navigation: grab intent values
            vm.isServer = intent.getBooleanExtra(EXTRA_BOOLEAN_IS_SERVER, false)
            vm.quizId = intent.getIntExtra(EXTRA_INT_QUIZ_ID, -1)
            val name = intent.getStringExtra(EXTRA_STRING_DEVICE_NAME) ?: context.getString(R.string.unknown_name)
            val quizName = intent.getStringExtra(EXTRA_STRING_QUIZ_NAME) ?: context.getString(R.string.unknown_quiz_name)
            vm.name = name
            vm.quizName = quizName

            val advertisedName = if (vm.isServer) context.getString(R.string.server_advertised_name, name, quizName) else name
            vm.connection = NearbyConnectionModel(this, lifecycleScope, advertisedName)
        }

        // Allows the creation of the fragments without the header previous arrow buttons
        val navController = findNavController(R.id.nav_host_fragment_activity_gameplay)
        val appBarConfiguration = AppBarConfiguration
            .Builder(R.id.FragmentGameplayQuiz,R.id.FragmentGameplayResults)
            .build()
        // Needed to remove the back arrow for the first fragment, without it the back arrow returns to the previous activity
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Stops the exit button appearing on the activity gameplay
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.permissionFragment || destination.id == R.id.connectClientSetName ||
                destination.id == R.id.connectClientFragment || destination.id == R.id.connectServerFragment ||
                destination.id == R.id.FragmentGameplayResults) {
                binding.exitFab.visibility = View.GONE
            } else {
                binding.exitFab.visibility = View.VISIBLE
            }
        }

        // Exit button for the activity gameplay
        binding.exitFab.setOnClickListener {
            exitQuiz(it.context)
        }

        // TODO uncomment this if done testing!
        // Stops the user from being able to press back and mess up the game accidentally.
        navController.addOnDestinationChangedListener { _, destination, _ ->
            backButtonDisabled = destination.id == R.id.FragmentGameplayQuiz || destination.id == R.id.FragmentGameplayResults
        }
    }

    /**
     * Ends the game if the user is sure.
     */
    private fun exitQuiz(context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setNegativeButton(R.string.no) { _, _ -> }    // Make a "No" option and set action if the user selects "No"
        builder.setPositiveButton(R.string.yes) { _, _ ->     // Make a "Yes" option and set action if the user selects "Yes"
            val intent = Intent(context, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
        builder.setTitle("Exit Game?")
        builder.setMessage("Are you sure you want to abandon this game?")
        builder.create().show()
    }

// Commented out to remove the header back arrow for fragments
//    override fun onSupportNavigateUp(): Boolean {
//        val navController = findNavController(R.id.nav_host_fragment_activity_gameplay)
//        return navController.navigateUp(appBarConfiguration)
//                || super.onSupportNavigateUp()
//    }

    // TODO uncomment this if done testing!
    override fun onBackPressed() {
        if (!backButtonDisabled) {
            super.onBackPressed()
        }
    }

    override fun onStop() {
        super.onStop()
        if (isFinishing) {
            vm.connection.stopAllEndpoints()
        }
    }
}
