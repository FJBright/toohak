package com.example.notKahoot

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.JsonWriter
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.example.notKahoot.databinding.ActivityMainBinding
import com.example.notKahoot.utilities.SCORES_DATA_FILENAME
import com.example.notKahoot.viewModel.QuestionViewModel
import com.example.notKahoot.viewModel.QuizViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.FileNotFoundException
import java.io.OutputStreamWriter

/**
 *
 */
class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    private lateinit var binding: ActivityMainBinding

    private lateinit var mQuizViewModel: QuizViewModel
    private lateinit var mQuestionViewModel: QuestionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navController = findNavController(R.id.nav_host_fragment_activity_main)

        // Room View Models
        mQuizViewModel = ViewModelProvider(this)[QuizViewModel::class.java]
        mQuestionViewModel = ViewModelProvider(this)[QuestionViewModel::class.java]

        val bottomNavigationView: BottomNavigationView = binding.navView
        bottomNavigationView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.FragmentGameplayQuiz) {
                bottomNavigationView.visibility = View.GONE
            } else {
                bottomNavigationView.visibility = View.VISIBLE
            }
        }

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_quiz_list, R.id.navigation_scoreboard, R.id.navigation_settings
            )
        )
        // Commented out to remove back button on fragment headers
         setupActionBarWithNavController(navController, appBarConfiguration)

        scoresFileCheck()
    }

    // Checks to see if the scores file is within the apps storage if not then creates one with
    // default values
    private fun scoresFileCheck() {
        try {
            openFileInput(SCORES_DATA_FILENAME)
        } catch (e: FileNotFoundException) {
            val file = openFileOutput(SCORES_DATA_FILENAME, Context.MODE_PRIVATE)
            val writer = JsonWriter(OutputStreamWriter(file))
            writer.setIndent("  ")
            defaultWrite(writer)
            writer.close()
        }
    }


    // Creates the JSON object with default values (0) for the each of the scores.
    private fun defaultWrite(writer: JsonWriter) {
        writer.beginObject()
        // TODO writer requires a string not string resources int
        writer.name("wins").value(0)
        writer.name("losses").value(0)
        writer.name("ties").value(0)
        writer.endObject()
    }

    // Doesn't seem necessary for the navigation to previous fragments but better to have it just in case
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration)
    }

    // TODO decide if this would be useful (is this necessary for the actionbar or does the navbar suit?)
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.another_menu, menu)
        return true
    }

    // TODO decide if this would be useful (is this necessary for the actionbar or does the navbar suit?)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.another_item_1 -> {
                deleteAllQuizzes()
            }

            R.id.another_item_2 -> {
                deleteAllQuestions()
            }

            R.id.another_item_3 -> {
                deleteScoreboard()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Logic to ask the user if they want to delete all items (quizzes)
     */
    private fun deleteScoreboard() {
        val builder = AlertDialog.Builder(this)
        builder.setNegativeButton(R.string.no) { _, _ -> }    // Make a "No" option and set action if the user selects "No"
        builder.setPositiveButton(R.string.yes) { _, _ ->     // Make a "Yes" option and set action if the user selects "Yes"
            deleteAllScores()
            showToast(R.string.delete_score_success)
        }
        builder.setTitle("Reset All Scores?")  // Set the title of the prompt with a sentence saying the first name of the user inside the app (using template string)
        builder.setMessage("Are you sure you want to reset all your score data?")  // Set the message of the prompt with a sentence saying the first name of the user inside the app (using template string)
        builder.create().show()  // Create a prompt with the configuration above to ask the user (the real app user which is human)
    }

    /**
     * Logic to delete/reset all the players wins/loss/ties data
     */
    private fun deleteAllScores() {
        val file = openFileOutput(SCORES_DATA_FILENAME, Context.MODE_PRIVATE)
        val writer = JsonWriter(OutputStreamWriter(file))
        writer.setIndent("  ")
        defaultWrite(writer)
        writer.close()
    }

    /**
     * Logic to ask the user if they want to delete all items (quizzes)
     */
    private fun deleteAllQuizzes() {
        val builder = AlertDialog.Builder(this)
        builder.setNegativeButton(R.string.no) { _, _ -> }    // Make a "No" option and set action if the user selects "No"
        builder.setPositiveButton(R.string.yes) { _, _ ->     // Make a "Yes" option and set action if the user selects "Yes"
            mQuizViewModel.deleteAllQuizzes()    // Execute : delete all users
            showToast(R.string.delete_quizzes_success)
            // Note: No need to navigate app user to List Fragment since deleting all users takes place at List Fragment.
        }
        builder.setTitle(R.string.delete_quizzes)  // Set the title of the prompt with a sentence saying the first name of the user inside the app (using template string)
        builder.setMessage(R.string.delete_everything)  // Set the message of the prompt with a sentence saying the first name of the user inside the app (using template string)
        builder.create().show()  // Create a prompt with the configuration above to ask the user (the real app user which is human)
    }

    /**
     * Logic to ask the user if they want to delete all items (questions)
     */
    private fun deleteAllQuestions() {
        val builder = AlertDialog.Builder(this)
        builder.setNegativeButton(R.string.no) { _, _ -> }    // Make a "No" option and set action if the user selects "No"
        builder.setPositiveButton(R.string.yes) { _, _ ->     // Make a "Yes" option and set action if the user selects "Yes"
            mQuestionViewModel.deleteAllQuestions()    // Execute : delete all users
            showToast(R.string.delete_questions_success)
            // Note: No need to navigate app user to List Fragment since deleting all users takes place at List Fragment.
        }
        builder.setTitle(R.string.delete_questions)  // Set the title of the prompt with a sentence saying the first name of the user inside the app (using template string)
        builder.setMessage(R.string.delete_everything)  // Set the message of the prompt with a sentence saying the first name of the user inside the app (using template string)
        builder.create().show()  // Create a prompt with the configuration above to ask the user (the real app user which is human)
    }

    private fun showToast(msg: Int) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

}
