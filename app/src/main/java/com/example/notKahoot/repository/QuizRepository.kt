package com.example.notKahoot.repository

import androidx.lifecycle.LiveData
import com.example.notKahoot.data.QuizDAO
import com.example.notKahoot.model.QuizModel

/**
 * Repository abstracts access to multiple data sources. However this is not the part of the Architecture Component libraries.
 */
class QuizRepository(private val quizDao: QuizDAO) {

    val readAllData: LiveData<List<QuizModel>> = quizDao.readAllData()
    val readNotEmpty: LiveData<List<QuizModel>> = quizDao.getNotEmptyQuizzes()

    suspend fun addQuiz(quiz: QuizModel) {
        quizDao.insert(quiz)
    }

    fun getQuiz(quizId: Int): QuizModel? {
        return quizDao.getQuiz(quizId)
    }

    suspend fun updateQuiz(quiz: QuizModel) {
        quizDao.update(quiz)
    }

    suspend fun deleteQuiz(quiz: QuizModel) {
        quizDao.delete(quiz)
    }

    fun getAllQuizzes() {
        quizDao.getAll()
    }

    fun getQuizCount() {
        quizDao.getCount()
    }

    suspend fun deleteAllQuizzes() {
        quizDao.deleteAllQuizzes()
    }

}
