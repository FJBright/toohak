package com.example.notKahoot.ui.questions.add

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.notKahoot.databinding.FragmentQuestionAddBinding
import com.example.notKahoot.model.QuestionModel
import com.example.notKahoot.viewModel.QuestionViewModel

class AddQuestionFragment : Fragment() {

    private var _binding: FragmentQuestionAddBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<AddQuestionFragmentArgs>()

    private lateinit var mQuestionViewModel: QuestionViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentQuestionAddBinding.inflate(inflater, container, false)

        mQuestionViewModel = ViewModelProvider(this)[QuestionViewModel::class.java]

        binding.cancelBtn.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.addBtn.setOnClickListener {
            insertDataToDatabase()
        }

        return binding.root
    }

    private fun insertDataToDatabase() {
        val question = binding.addQuestionET.text.toString()
        val option1 = binding.addOption1ET.text.toString()
        val option2 = binding.addOption2ET.text.toString()
        val option3 = binding.addOption3ET.text.toString()
        val option4 = binding.addOption4ET.text.toString() // <- This is correct

        // Check if the inputCheck function is true
        if (inputCheck(question, option1, option4)) {
            // Create Question Object
            val user = QuestionModel(
                0,
                args.currentQuiz.id,
                question,
                option1,
                option2,
                option3,
                option4,
                option4)

            // Add Data to database
            mQuestionViewModel.addQuestion(user)
            Toast.makeText(requireContext(), "Successfully added!", Toast.LENGTH_LONG).show()
            // Navigate back
            findNavController().navigateUp()
        } else {
            Toast.makeText(requireContext(), "Please fill all mandatory fields (*)!", Toast.LENGTH_LONG).show()
        }
    }

    private fun inputCheck(question: String,
                           option1: String,
                           option4: String): Boolean {
        return !(TextUtils.isEmpty(question)
                || TextUtils.isEmpty(option1)
                || TextUtils.isEmpty(option4))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // <- whenever we destroy our fragment, _binding is set to null. Hence it will avoid memory leaks.
    }

}