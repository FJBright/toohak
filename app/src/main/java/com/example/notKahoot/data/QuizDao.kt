package com.example.notKahoot.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.notKahoot.model.QuizModel
import kotlinx.coroutines.flow.Flow

/**
 * Our DAO declares methods that will support these operations.
 * But it doesn't implement them. The whole point of an ORM is
 * to automatically define these implementations based on our
 * entities. So, our DAO is only an interface:
 **/
@Dao
interface QuizDAO {
    @Insert
    suspend fun insert(quiz: QuizModel)

    @Update
    suspend fun update(quiz: QuizModel)

    @Delete
    suspend fun delete(quiz: QuizModel)

    // A Flow is a type (defined in kotlinx-coroutines) that emits values sequentially over time,
    // so it allows you to observe changes in values generated from async operations, such as
    // database queries.
    @Query("SELECT * FROM quiz_table")
    fun getAll(): Flow<List<QuizModel>>

    @Query("SELECT * FROM quiz_table WHERE id=:quizId")
    fun getQuiz(quizId: Int): QuizModel?

    @Query("SELECT COUNT(*) FROM quiz_table")
    fun getCount(): Flow<Int>

    @Query("SELECT * from quiz_table ORDER BY id ASC") // <- Add a query to fetch all users (in user_table) in ascending order by their IDs.
    fun readAllData(): LiveData<List<QuizModel>> // <- This means function return type is List. Specifically, a List of Quizzes.

    @Query("DELETE FROM quiz_table")
    suspend fun deleteAllQuizzes()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(quizzes: List<QuizModel>)

    // Removes duplicates caused by multiple questions and filters out empty quizzes for the quiz selection
    @Query("SELECT DISTINCT t1.id, t1.quizTitle, t1.totalQuestions FROM quiz_table t1 INNER JOIN question_table t2 ON t1.id = t2.quiz_id ORDER BY t1.id")
    fun getNotEmptyQuizzes(): LiveData<List<QuizModel>>
}
