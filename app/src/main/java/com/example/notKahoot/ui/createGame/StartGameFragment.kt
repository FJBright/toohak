package com.example.notKahoot.ui.createGame

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.example.notKahoot.R
import com.example.notKahoot.ui.createGame.playGame.ActivityGameplayQuiz

class StartGameFragment : Fragment() {

    private val args by navArgs<StartGameFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View = inflater.inflate(R.layout.fragment_create_game, container, false)

        view.findViewById<Button>(R.id.startGameButton)?.setOnClickListener {
            val intent = Intent(requireActivity(), ActivityGameplayQuiz::class.java)
            intent.putExtra(ActivityGameplayQuiz.EXTRA_BOOLEAN_IS_SERVER, true)
            intent.putExtra(ActivityGameplayQuiz.EXTRA_INT_QUIZ_ID, args.currentQuiz.id)
            intent.putExtra(ActivityGameplayQuiz.EXTRA_STRING_QUIZ_NAME, args.currentQuiz.quizTitle)
            startActivity(intent)
        }

        return view
    }
}