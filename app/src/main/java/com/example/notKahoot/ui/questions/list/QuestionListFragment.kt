package com.example.notKahoot.ui.questions.list

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notKahoot.R
import com.example.notKahoot.databinding.FragmentQuestionListBinding
import com.example.notKahoot.model.QuestionModel
import com.example.notKahoot.viewModel.QuestionViewModel

/**
 *
 **/
class QuestionListFragment : Fragment(), QuestionAdapter.OnQuestionListener {

    private var _binding: FragmentQuestionListBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<QuestionListFragmentArgs>()

    private lateinit var mQuestionViewModel: QuestionViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentQuestionListBinding.inflate(inflater, container, false)
        // val view = inflater.inflate(R.layout.fragment_list, container, false) // <- This is not required.

        // RecyclerView
        val adapter = QuestionAdapter(this)
        // val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerview) // <- This is replaced.
        val recyclerView = binding.questionRecyclerView
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // QuestionViewModel
        mQuestionViewModel = ViewModelProvider(this)[QuestionViewModel::class.java]

        mQuestionViewModel.readFilteredData = mQuestionViewModel.getAllQuestionsByQuizId(args.currentQuiz.id)
        mQuestionViewModel.readFilteredData.observe(viewLifecycleOwner, Observer { questionList ->
            adapter.setData(questionList)
        })

        binding.questionActionButton.setOnClickListener {
            val action = QuestionListFragmentDirections.actionQuestionsListToNavigationAddQuestionFragment(args.currentQuiz)
            findNavController().navigate(action)
        }

        // Add menu
        setHasOptionsMenu(true)
        return binding.root
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.delete_menu, menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_delete) {
            deleteAllQuestions()
        }
        return super.onOptionsItemSelected(item)
    }


    // Implement logic to delete one item
    private fun deleteOneQuestion(selectedQuestion: QuestionModel) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setNegativeButton(R.string.no) { _, _ -> }    // Make a "No" option and set action if the user selects "No"
        builder.setPositiveButton(R.string.yes) { _, _ ->     // Make a "Yes" option and set action if the user selects "Yes"
            mQuestionViewModel.deleteQuestion(selectedQuestion)
            showToast("Successfully removed ${selectedQuestion.question}")
        }
        builder.setTitle("Delete ${selectedQuestion.question}?")
        builder.setMessage(R.string.delete_one_question)
        builder.create().show()
    }


    /**
     * Logic to ask the user if they want to delete all items (questions)
     */
    private fun deleteAllQuestions() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setNegativeButton(R.string.no) { _, _ -> }    // Make a "No" option and set action if the user selects "No"
        builder.setPositiveButton(R.string.yes) { _, _ ->     // Make a "Yes" option and set action if the user selects "Yes"
            mQuestionViewModel.deleteAllQuestionsByQuiz(args.currentQuiz.id)
            Toast.makeText(
                requireContext(),
                R.string.delete_questions_success,
                Toast.LENGTH_SHORT)
                .show()
        }
        builder.setTitle(R.string.delete_questions)
        builder.setMessage(R.string.delete_everything)
        builder.create().show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // <- whenever we destroy our fragment, _binding is set to null. Hence it will avoid memory leaks.
    }


    override fun onQuestionClick(position: Int) {
        val questionList: List<QuestionModel>? = mQuestionViewModel.readFilteredData.value
        val currentItem = questionList?.get(position)
        val options = arrayOf("Edit", "Delete")
        val builder = AlertDialog.Builder(context)
        if (currentItem != null) {
            builder.setTitle("Manage ${currentItem.question}")
            builder.setItems(options) { _, optionId ->
                questionManagementOptions(optionId, currentItem)
            }
            builder.show()
        }
    }


    /**
     * Handles the interaction of a single recyclerView question item.
     */
    private fun questionManagementOptions(optionId: Int, currentItem: QuestionModel) {
        when (optionId) {
            0 -> {
                val action = QuestionListFragmentDirections.actionQuestionsListToNavigationUpdateQuestionFragment(currentItem)
                findNavController().navigate(action)
            }
            1 -> {
                deleteOneQuestion(currentItem)
            }
        }
    }


    private fun showToast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}