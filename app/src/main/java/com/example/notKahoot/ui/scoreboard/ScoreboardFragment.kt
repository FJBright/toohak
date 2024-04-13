package com.example.notKahoot.ui.scoreboard

import android.content.Context
import android.os.Bundle
import android.util.JsonReader
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.notKahoot.databinding.FragmentScoreboardBinding
import com.example.notKahoot.utilities.SCORES_DATA_FILENAME
import java.io.FileNotFoundException
import java.io.InputStreamReader


class ScoreboardFragment: Fragment() {

    private var _binding: FragmentScoreboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var textWins: TextView
    private lateinit var textLosses: TextView
    private lateinit var textTies: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentScoreboardBinding.inflate(inflater, container, false)

        textWins = binding.textNumWins
        textLosses = binding.textNumLosses
        textTies = binding.textNumTies

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        try{
            val file = activity?.openFileInput(SCORES_DATA_FILENAME)
            val reader = JsonReader(InputStreamReader(file))
            reader.beginObject()
            while (reader.hasNext()) {
                when(reader.nextName()) {
                    "wins" -> textWins.text = reader.nextString()
                    "losses" -> textLosses.text = reader.nextString()
                    "ties" -> textTies.text = reader.nextString()
                }
            }
            reader.endObject()
            reader.close()
        } catch (e: FileNotFoundException) {
            println("FILE READ FAILURE")
            return
        }

    }
}