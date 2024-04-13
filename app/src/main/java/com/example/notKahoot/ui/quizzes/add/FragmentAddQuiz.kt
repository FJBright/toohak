package com.example.notKahoot.ui.quizzes.add

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.notKahoot.databinding.FragmentQuizAddBinding
import com.example.notKahoot.model.QuizModel
import com.example.notKahoot.viewModel.QuizViewModel

class FragmentAddQuiz : Fragment() {

    private var _binding: FragmentQuizAddBinding? = null
    private val binding get() = _binding!!

    private lateinit var mQuizViewModel: QuizViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentQuizAddBinding.inflate(inflater, container, false)

        mQuizViewModel = ViewModelProvider(this)[QuizViewModel::class.java]

        binding.cancelBtn.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.addBtn.setOnClickListener {
            insertDataToDatabase()
        }

        return binding.root
    }

    private fun insertDataToDatabase() {
        // val firstName = addFirstName_et.text.toString() // viewBinding will automatically convert addFirstName_et to addFirstNameEt. Thus, addFirstName_et does not exist.
        // val firstName = addFirstNameEt.text.toString() // <- Error : Unresolved reference: addFirstNameEt
        val firstName = binding.addFirstNameEt.text.toString() // <- This is correct

        // Check if the inputCheck function is true
        if(inputCheck(firstName)) {
            // Create Quiz Object
            val user = QuizModel(0, firstName, 0) // <- Pass id, firstName, lastName, and age. Although id will be auto-generated because it is a primary key, we need to pass a value or zero (Don't worry, the Room library knows it is the primary key and is auto-generated).

            // Add Data to database
            mQuizViewModel.addQuiz(user)
            Toast.makeText(requireContext(), "Successfully added!", Toast.LENGTH_LONG).show()
            // Navigate back
            findNavController().navigateUp()
        } else {
            Toast.makeText(requireContext(), "Please fill out all fields!", Toast.LENGTH_LONG).show()
        }
    }

    private fun inputCheck(firstName: String): Boolean {
        return !(TextUtils.isEmpty(firstName))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // <- whenever we destroy our fragment, _binding is set to null. Hence it will avoid memory leaks.
    }

}