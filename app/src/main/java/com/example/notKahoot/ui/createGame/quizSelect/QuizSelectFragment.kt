package com.example.notKahoot.ui.createGame.quizSelect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notKahoot.R
import com.example.notKahoot.databinding.FragmentQuizListBinding
import com.example.notKahoot.model.QuizModel
import com.example.notKahoot.ui.quizzes.list.AdapterQuiz
import com.example.notKahoot.viewModel.QuizSelectViewModel

class QuizSelectFragment : Fragment(), AdapterQuiz.OnQuizListener {

    private var _binding: FragmentQuizListBinding? = null
    private val binding get() = _binding!!

    private lateinit var mQuizViewModel: QuizSelectViewModel

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
        mQuizViewModel = ViewModelProvider(this)[QuizSelectViewModel::class.java]
        mQuizViewModel.readNotEmpty.observe(viewLifecycleOwner, Observer { quizList ->
            adapter.setData(quizList)
        })

        binding.floatingActionButton.visibility = View.GONE

        return binding.root
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // <- whenever we destroy our fragment, _binding is set to null. Hence it will avoid memory leaks.
    }


    override fun onQuizClick(position: Int) {
        val quizList: List<QuizModel>? = mQuizViewModel.readNotEmpty.value
        val currentItem = quizList?.get(position)
        val args = bundleOf(Pair("currentQuiz", currentItem))
        findNavController().navigate(R.id.action_quizSelectFragment_to_CreateGameFragment, args)
    }
}
