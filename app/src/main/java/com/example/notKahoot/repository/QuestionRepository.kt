package com.example.notKahoot.repository

import androidx.lifecycle.LiveData
import com.example.notKahoot.data.QuestionDao
import com.example.notKahoot.model.QuestionModel

class QuestionRepository(private val questionDao: QuestionDao) {

    val readAllData: LiveData<List<QuestionModel>> = questionDao.readAllData()

    suspend fun filterByQuiz(quizId: Int): List<QuestionModel> {
        return questionDao.filterByQuiz(quizId)
    }

    suspend fun addQuestion(question: QuestionModel) {
        questionDao.insert(question)
    }

    suspend fun updateQuestion(question: QuestionModel) {
        questionDao.update(question)
    }

    suspend fun deleteQuestion(question: QuestionModel) {
        questionDao.delete(question)
    }

    fun getAllQuestionsByQuizId(quiz_id: Int): LiveData<List<QuestionModel>> {
        return questionDao.getAllQuestionsByQuizId(quiz_id)
    }

    fun getQuestionCount() {
        questionDao.getCount()
    }

    suspend fun deleteAllQuestionsByQuiz(quiz_id: Int) {
        questionDao.deleteAllQuestionsByQuiz(quiz_id)
    }

    suspend fun deleteAllQuestions() {
        questionDao.deleteAllQuestions()
    }
}
