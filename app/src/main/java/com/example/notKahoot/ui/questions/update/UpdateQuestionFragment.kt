package com.example.notKahoot.ui.questions.update

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.notKahoot.R
import com.example.notKahoot.databinding.FragmentQuestionUpdateBinding
import com.example.notKahoot.model.QuestionModel
import com.example.notKahoot.viewModel.QuestionViewModel

class UpdateQuestionFragment : Fragment() {

    private var _binding: FragmentQuestionUpdateBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<UpdateQuestionFragmentArgs>()

    private lateinit var mQuestionViewModel: QuestionViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentQuestionUpdateBinding.inflate(inflater, container, false)

        mQuestionViewModel = ViewModelProvider(this)[QuestionViewModel::class.java]

        binding.addQuestionET.setText(args.currentQuestion.question)
        binding.addOption1ET.setText(args.currentQuestion.option_one)
        binding.addOption2ET.setText(args.currentQuestion.option_two)
        binding.addOption3ET.setText(args.currentQuestion.option_three)
        binding.addOption4ET.setText(args.currentQuestion.option_four)

        binding.cancelBtn.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.updateBtn.setOnClickListener {
            updateItem()
        }

        // Add menu
        setHasOptionsMenu(true)

        return binding.root
    }

    private fun updateItem() {
        val question = binding.addQuestionET.text.toString()
        val option1 = binding.addOption1ET.text.toString()
        val option2 = binding.addOption2ET.text.toString()
        val option3 = binding.addOption3ET.text.toString()
        val option4 = binding.addOption4ET.text.toString()

        if (inputCheck(question, option1, option4)) {
            // Create Question Object
            val updatedQuestion = QuestionModel(
                args.currentQuestion.id,
                args.currentQuestion.quiz_id,
                question,
                option1,
                option2,
                option3,
                option4,
                option4)

            // Update Current Question
            mQuestionViewModel.updateQuestion(updatedQuestion)
            Toast.makeText(requireContext(), "Updated Successfully!", Toast.LENGTH_SHORT).show()

            // Navigate back to List Fragment
            findNavController().navigateUp()
        } else {
            Toast.makeText(requireContext(), "Please fill all mandatory fields (*)!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun inputCheck(question: String,
                           option1: String,
                           option4: String): Boolean {
        return !(TextUtils.isEmpty(question)
                || TextUtils.isEmpty(option1)
                || TextUtils.isEmpty(option4))
    }

    // Inflate the layout to our menu
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.delete_menu, menu)
    }

    // Handle clicks on menu items
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_delete) {
            deleteQuestion()
        }
        return super.onOptionsItemSelected(item)
    }

    // Implement logic to delete a question
    private fun deleteQuestion() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setPositiveButton(R.string.yes) { _, _ ->     // Make a "Yes" option and set action if the question selects "Yes"
            mQuestionViewModel.deleteQuestion(args.currentQuestion)    // Execute : delete question

            showToast("Successfully removed ${args.currentQuestion.question}", requireContext())
            findNavController().navigateUp() // Navigate to List Fragment after deleting a question
        }
        builder.setNegativeButton(R.string.no) { _, _ -> }    // Make a "No" option and set action if the question selects "No"
        builder.setTitle("Delete ${args.currentQuestion.question}?")  // Set the title of the prompt with a sentence saying the first name of the question inside the app (using template string)
        builder.setMessage("Are you sure to remove this question?")  // Set the message of the prompt with a sentence saying the first name of the question inside the app (using template string)
        builder.create().show()  // Create a prompt with the configuration above to ask the question (the real app question which is human)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // <- whenever we destroy our fragment, _binding is set to null. Hence it will avoid memory leaks.
    }

    private fun showToast(msg: String, context: Context) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
}