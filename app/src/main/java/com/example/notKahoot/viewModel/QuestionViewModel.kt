package com.example.notKahoot.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.notKahoot.data.QuizDatabase
import com.example.notKahoot.model.QuestionModel
import com.example.notKahoot.repository.QuestionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QuestionViewModel(application: Application): AndroidViewModel(application) {

    lateinit var readFilteredData: LiveData<List<QuestionModel>>
    private val repository: QuestionRepository

    init {
        val questionDao = QuizDatabase.getDatabase(application).questionDao()
        repository= QuestionRepository(questionDao)
    }

    suspend fun filterByQuiz(quizId: Int): List<QuestionModel> {
        return repository.filterByQuiz(quizId)
    }

    fun addQuestion(question: QuestionModel) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addQuestion(question)
        }
    }

    fun updateQuestion(question: QuestionModel) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateQuestion(question)
        }
    }

    fun deleteQuestion(question: QuestionModel) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteQuestion(question)
        }
    }

    fun getAllQuestionsByQuizId(quiz_id: Int): LiveData<List<QuestionModel>> {
        return repository.getAllQuestionsByQuizId(quiz_id)
    }

    fun deleteAllQuestionsByQuiz(quiz_id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAllQuestionsByQuiz(quiz_id)
        }
    }

    fun deleteAllQuestions() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAllQuestions()
        }
    }
}