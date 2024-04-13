package com.example.notKahoot.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.notKahoot.model.QuestionModel
import com.example.notKahoot.model.QuizModel
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {
    @Insert
    suspend fun insert(question: QuestionModel)

    @Update
    suspend fun update(question: QuestionModel)

    @Delete
    suspend fun delete(question: QuestionModel)

    @Query("SELECT * from question_table WHERE quiz_id=:quiz_id ORDER BY id ASC")
    fun getAllQuestionsByQuizId(quiz_id: Int): LiveData<List<QuestionModel>>

    @Query("SELECT * from question_table WHERE quiz_id=:quiz_id ORDER BY id ASC")
    suspend fun filterByQuiz(quiz_id: Int): List<QuestionModel>

    @Query("SELECT COUNT(*) FROM question_table")
    fun getCount(): Flow<Int>

    @Query("SELECT * from question_table ORDER BY id ASC") // <- Add a query to fetch all users (in user_table) in ascending order by their IDs.
    fun readAllData(): LiveData<List<QuestionModel>> // <- This means function return type is List. Specifically, a List of Questions.

    @Query("DELETE FROM question_table WHERE quiz_id=:quiz_id")
    suspend fun deleteAllQuestionsByQuiz(quiz_id: Int)

    @Query("DELETE FROM question_table")
    suspend fun deleteAllQuestions()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<QuestionModel>)
}
