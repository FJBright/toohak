package com.example.notKahoot.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.notKahoot.model.QuestionModel
import com.example.notKahoot.model.QuizModel
import com.example.notKahoot.utilities.DATABASE_NAME
import com.example.notKahoot.utilities.QUESTION_DATA_FILENAME
import com.example.notKahoot.utilities.QUIZ_DATA_FILENAME
import com.example.notKahoot.workers.QuestionSeedDatabaseWorker
import com.example.notKahoot.workers.QuestionSeedDatabaseWorker.Companion.QUESTION_KEY_FILENAME
import com.example.notKahoot.workers.QuizSeedDatabaseWorker
import com.example.notKahoot.workers.QuizSeedDatabaseWorker.Companion.QUIZ_KEY_FILENAME


/**
 * Database represents database and contains the database holder and server the main access
 * point for the underlying connection to your app's persisted, relational data.
 */
@Database(entities = [QuizModel::class, QuestionModel::class], version = 1)
abstract class QuizDatabase: RoomDatabase() {
    abstract fun quizDao(): QuizDAO
    abstract fun questionDao(): QuestionDao

    companion object {
        @Volatile
        private var INSTANCE: QuizDatabase? = null

        fun getInstance(): QuizDatabase? {
            return INSTANCE
        }

        fun getDatabase(context: Context): QuizDatabase{
            val tempInstance = INSTANCE

            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    QuizDatabase::class.java,
                    DATABASE_NAME
                ).addCallback(
                    object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)

                            val requestQuizzes = OneTimeWorkRequestBuilder<QuizSeedDatabaseWorker>()
                                .setInputData(workDataOf(QUIZ_KEY_FILENAME to QUIZ_DATA_FILENAME))
                                .build()
                            WorkManager.getInstance(context).enqueue(requestQuizzes)

                            val requestQuestions = OneTimeWorkRequestBuilder<QuestionSeedDatabaseWorker>()
                                .setInputData(workDataOf(QUESTION_KEY_FILENAME to QUESTION_DATA_FILENAME))
                                .build()
                            WorkManager.getInstance(context).enqueue(requestQuestions)
                        }
                    }
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}
