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
class QuizViewModel(application: Application): AndroidViewModel(application) {

    var readAllData: LiveData<List<QuizModel>>
    private val repository: QuizRepository

    init {
        val quizDao = QuizDatabase.getDatabase(application).quizDao()
        repository= QuizRepository(quizDao)
        readAllData = repository.readAllData
    }

    fun addQuiz(quiz: QuizModel) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addQuiz(quiz)
        }
    }

    /// Must be called from non-main thread
    fun getQuiz(quizId: Int) {
        repository.getQuiz(quizId)
    }

    fun updateQuiz(quiz: QuizModel) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateQuiz(quiz)
        }
    }

    fun deleteQuiz(quiz: QuizModel) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteQuiz(quiz)
        }
    }

    fun deleteAllQuizzes() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAllQuizzes()
        }
    }

}
