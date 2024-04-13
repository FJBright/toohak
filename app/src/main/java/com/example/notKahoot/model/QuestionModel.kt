package com.example.notKahoot.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

/**
 *
 **/
@Parcelize
@Entity(tableName = "question_table", foreignKeys = [ForeignKey(entity=QuizModel::class,
    parentColumns = arrayOf("id"),
    childColumns = arrayOf("quiz_id"),
    onDelete = ForeignKey.CASCADE)]
)
class QuestionModel (
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo var quiz_id: Int,
    @ColumnInfo var question: String,
    @ColumnInfo var option_one: String,
    @ColumnInfo var option_two: String,
    @ColumnInfo var option_three: String,
    @ColumnInfo var option_four: String,
    @ColumnInfo var correct_option: String,
): Parcelable {
    val allOptions: List<String> get() = listOf(option_one, option_two, option_three, option_four)
}
