package com.example.notKahoot.ui.quizzes.update

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
import com.example.notKahoot.databinding.FragmentQuizUpdateBinding
import com.example.notKahoot.model.QuizModel
import com.example.notKahoot.viewModel.QuizViewModel

class FragmentUpdateQuiz : Fragment() {

    private var _binding: FragmentQuizUpdateBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<FragmentUpdateQuizArgs>()

    private lateinit var mQuizViewModel: QuizViewModel

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentQuizUpdateBinding.inflate(inflater, container, false)

        mQuizViewModel = ViewModelProvider(this)[QuizViewModel::class.java]

        binding.updateQuizTitle.setText(args.currentQuiz.quizTitle)

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
        val quizTitle = binding.updateQuizTitle.text.toString()

        if (inputCheck(quizTitle)) {
            // Create Quiz Object
            val updatedQuiz = QuizModel(args.currentQuiz.id, quizTitle, 0)

            // Update Current Quiz
            mQuizViewModel.updateQuiz(updatedQuiz)
            Toast.makeText(requireContext(), "Updated Successfully !", Toast.LENGTH_SHORT).show()

            // Navigate back to List Fragment
            findNavController().navigateUp()
        } else {
            Toast.makeText(requireContext(), "Please fill all fields !", Toast.LENGTH_SHORT).show()
        }
    }

    private fun inputCheck(quizTitle: String): Boolean {
        return !(TextUtils.isEmpty(quizTitle))
    }

    // Inflate the layout to our menu
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.delete_menu, menu)
    }

    // Handle clicks on menu items
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_delete) {
            deleteQuiz()
        }
        return super.onOptionsItemSelected(item)
    }

    // Implement logic to delete a quiz
    private fun deleteQuiz() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setPositiveButton(R.string.yes) { _, _ ->     // Make a "Yes" option and set action if the quiz selects "Yes"
            mQuizViewModel.deleteQuiz(args.currentQuiz)    // Execute : delete quiz

            showToast("Successfully removed ${args.currentQuiz.quizTitle}", requireContext())
            findNavController().navigateUp() // Navigate to List Fragment after deleting a quiz
        }
        builder.setNegativeButton(R.string.no) { _, _ -> }    // Make a "No" option and set action if the quiz selects "No"
        builder.setTitle("Delete ${args.currentQuiz.quizTitle}?")  // Set the title of the prompt with a sentence saying the first name of the quiz inside the app (using template string)
        builder.setMessage("Are you sure to remove ${args.currentQuiz.quizTitle}?")  // Set the message of the prompt with a sentence saying the first name of the quiz inside the app (using template string)
        builder.create().show()  // Create a prompt with the configuration above to ask the quiz (the real app quiz which is human)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // <- whenever we destroy our fragment, _binding is set to null. Hence it will avoid memory leaks.
    }

    private fun showToast(msg: String, context: Context) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
}