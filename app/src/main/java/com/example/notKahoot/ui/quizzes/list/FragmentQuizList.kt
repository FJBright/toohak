package com.example.notKahoot.ui.quizzes.list

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notKahoot.R
import com.example.notKahoot.databinding.FragmentQuizListBinding
import com.example.notKahoot.model.QuizModel
import com.example.notKahoot.viewModel.QuizViewModel

class FragmentQuizList : Fragment(), AdapterQuiz.OnQuizListener {

    private var _binding: FragmentQuizListBinding? = null
    private val binding get() = _binding!!

    private lateinit var mQuizViewModel: QuizViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentQuizListBinding.inflate(inflater, container, false)
        // val view = inflater.inflate(R.layout.fragment_list, container, false) // <- This is not required.

        // RecyclerView
        val adapter = AdapterQuiz(this)
        // val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerview) // <- This is replaced.
        val recyclerView = binding.recyclerView
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // QuizViewModel
        mQuizViewModel = ViewModelProvider(this)[QuizViewModel::class.java]
        mQuizViewModel.readAllData.observe(viewLifecycleOwner, Observer { quizList ->
            adapter.setData(quizList)
        })

        binding.floatingActionButton.setOnClickListener {
            findNavController().navigate(R.id.action_listFragment_to_addFragment)
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
            deleteAllQuizzes()
        }
        return super.onOptionsItemSelected(item)
    }


    // Implement logic to delete one item
    private fun deleteOneQuiz(selectedQuiz: QuizModel) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setNegativeButton(R.string.no) { _, _ -> }    // Make a "No" option and set action if the user selects "No"
        builder.setPositiveButton(R.string.yes) { _, _ ->     // Make a "Yes" option and set action if the user selects "Yes"
            mQuizViewModel.deleteQuiz(selectedQuiz)
            showToast("Successfully removed ${selectedQuiz.quizTitle}")
        }
        builder.setTitle("Delete ${selectedQuiz.quizTitle}?")
        builder.setMessage(R.string.delete_one_quiz)
        builder.create().show()
    }


    /**
     * Logic to ask the user if they want to delete all items (quizzes)
     */
    private fun deleteAllQuizzes() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setNegativeButton(R.string.no) { _, _ -> }    // Make a "No" option and set action if the user selects "No"
        builder.setPositiveButton(R.string.yes) { _, _ ->     // Make a "Yes" option and set action if the user selects "Yes"
            mQuizViewModel.deleteAllQuizzes()
            Toast.makeText(
                    requireContext(),
                    "Successfully removed everything",
                    Toast.LENGTH_SHORT)
                    .show()
        }
        builder.setTitle("Delete All Quizzes?")
        builder.setMessage("Are you sure you want to remove everything?")
        builder.create().show()  // Create a prompt with the configuration above to ask the user (the real app user which is human)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // <- whenever we destroy our fragment, _binding is set to null. Hence it will avoid memory leaks.
    }


    override fun onQuizClick(position: Int) {
        val quizList: List<QuizModel>? = mQuizViewModel.readAllData.value
        val currentItem = quizList?.get(position)

        val options = arrayOf("Play", "Manage Questions", "Edit", "Delete")
        val builder = AlertDialog.Builder(context)
        if (currentItem != null) {
            builder.setTitle("Manage ${currentItem.quizTitle}")
            builder.setItems(options) { _, optionId ->
                quizManagementOptions(optionId, currentItem)
            }
            builder.show()
        } else {
            showToast("There has been an error, please reload the app.")
        }
    }


    /**
     * Handles the interaction of a single recyclerView quiz item.
     */
    private fun quizManagementOptions(optionId: Int, currentItem: QuizModel) {
        when (optionId) {
            0 -> {
                // Examples of navigation with args defined in the navigation graph xml
                val action = FragmentQuizListDirections.actionNavigationQuizListToCreateGameFragment(currentItem)
                findNavController().navigate(action)
            }
            1 -> {
                val action = FragmentQuizListDirections.actionNavigationQuizListToQuestionsList(currentItem)
                findNavController().navigate(action)
            }
            2 -> {
                val action = FragmentQuizListDirections.actionListFragmentToUpdateFragment(currentItem)
                findNavController().navigate(action)
            }
            3 -> {
                deleteOneQuiz(currentItem)
            }
        }
    }


    private fun showToast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}
