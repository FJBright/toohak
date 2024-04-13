package com.example.notKahoot.ui.createGame.playGame

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.JsonReader
import android.util.JsonWriter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notKahoot.MainActivity
import com.example.notKahoot.R
import com.example.notKahoot.databinding.FragmentGameplayResultsBinding
import com.example.notKahoot.utilities.SCORES_DATA_FILENAME
import java.io.FileNotFoundException
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class GameplayResultsFragment : Fragment() {

    private val activityVm: ActivityGameplayViewModel by activityViewModels()

    private var _binding: FragmentGameplayResultsBinding? = null

    private var results: List<GameplayQuizFragment.QuizResultsPayload.IndividualResults>? = null

    private var currentWins = 0
    private var currentLosses = 0
    private var currentTies = 0

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGameplayResultsBinding.inflate(inflater, container, false)

        var thisPlayerAnswered = 0
        var thisPlayerAnsweredCorrect = 0

        readCurrentScores()

        val results = activityVm.quizResults?.results?.toMutableList()
        results?.let {
            if (it.find { it.id == activityVm.uuid } == null) {
                // If you didn't answer any questions
                results.add(GameplayQuizFragment.QuizResultsPayload.IndividualResults(activityVm.uuid, activityVm.connection.name, 0, 0))
            } else {
                thisPlayerAnswered = it.find { it.id == activityVm.uuid }!!.numAnswered
                thisPlayerAnsweredCorrect = it.find { it.id == activityVm.uuid }!!.numCorrect
            }
        }

        var highestScore = 0
        var sameScoreCounter = 0

        if (results != null) {
            for (i in results) {
                // Assumes the first result is the highest score
                // If this is not true this is really bad
                if (i.numCorrect > highestScore) {
                    highestScore = i.numCorrect
                }

                // If this player has the highest score but another player has it then they have drawn
                if (i.numCorrect == highestScore && thisPlayerAnsweredCorrect == highestScore && highestScore != 0) {
                    sameScoreCounter += 1
                }
            }

            val resultState = if (thisPlayerAnsweredCorrect == highestScore && highestScore != 0 && sameScoreCounter == 1) {
                0 // WIN
            } else if (sameScoreCounter >= 2) {
                1 // DRAW
            } else {
                2 // LOSS
            }

            writeScore(resultState)
        }

        binding.btnShare.setOnClickListener{
            val intent = Intent(Intent.ACTION_SEND)
            intent.setType("text/plain")
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.quiz_complete_subject))
            intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.quiz_complete_text).format(thisPlayerAnsweredCorrect, thisPlayerAnswered))
            startActivity(Intent.createChooser(intent, getString(R.string.share_with)))
        }

        val layoutManager = LinearLayoutManager(requireContext())
        val resultAdapter = ResultAdapter(results, activityVm.uuid)

        val recycler = binding.resultRecycler
        recycler.layoutManager = layoutManager
        recycler.adapter = resultAdapter

        return binding.root
    }

    // TODO stop one user exiting from closing other users out of the results page
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        binding.tvNameResult.text = context.getString(R.string.congratulations_name_here,
            activityVm.name ?: context.getString(R.string.unknown_name)
        )

        results?.find { it.id == activityVm.uuid }?.let {
            binding.currentScore.text = context.getString(R.string.self_score_description, it.numCorrect, it.numAnswered)
        } ?: run {
            binding.currentScore.visibility = View.GONE
        }

        binding.btnExit.setOnClickListener {
            // Reset the score when the game finishes
            // Go to homepage
            val intent = Intent(activity, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun readCurrentScores() {
        try {
            val file = requireActivity().openFileInput(SCORES_DATA_FILENAME)
            val reader = JsonReader(InputStreamReader(file))
            reader.beginObject()
            while(reader.hasNext()) {
                when(reader.nextName()) {
                    "wins" -> currentWins = reader.nextInt()
                    "losses" -> currentLosses = reader.nextInt()
                    "ties" -> currentTies = reader.nextInt()
                }
            }
        } catch (e: FileNotFoundException) {
            println("FILE READ FAILURE")
            return
        }
    }

    private fun writeScore(quizOutcome: Int) {
        try {
            val file = requireActivity().openFileOutput(SCORES_DATA_FILENAME, Context.MODE_PRIVATE)
            val writer = JsonWriter(OutputStreamWriter(file))
            writer.setIndent("  ")
            writer.beginObject()
            when(quizOutcome) {
                0 -> {writer.name("wins").value(currentWins + 1)
                    writer.name("losses").value(currentLosses)
                    writer.name("ties").value(currentTies)}
                1 -> {writer.name("ties").value(currentTies + 1)
                    writer.name("losses").value(currentLosses)
                    writer.name("wins").value(currentWins)}
                2 -> {writer.name("losses").value(currentLosses + 1)
                    writer.name("wins").value(currentWins)
                    writer.name("ties").value(currentTies)}
            }
            writer.endObject()
            writer.close()
        } catch (e: FileNotFoundException) {
            println("FILE NOT FOUND")
            return
        }
    }
}
