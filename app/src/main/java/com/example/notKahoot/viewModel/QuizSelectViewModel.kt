package com.example.notKahoot.viewModel

import android.app.Application
import androidx.lifecycle.*
import com.example.notKahoot.data.QuizDatabase
import com.example.notKahoot.model.QuestionModel
import com.example.notKahoot.model.QuizModel
import com.example.notKahoot.repository.QuizRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Provides quizs data to the UI and survive configuration changes.
 * A ViewModel acts as a communication center between the Repository and the UI.
 */
class QuizSelectViewModel(application: Application): AndroidViewModel(application) {

    var readNotEmpty: LiveData<List<QuizModel>>
//        get() = _quizzes
    private val repository: QuizRepository

    init {
        val quizDao = QuizDatabase.getDatabase(application).quizDao()
        repository= QuizRepository(quizDao)
        readNotEmpty = repository.readNotEmpty
    }
}
