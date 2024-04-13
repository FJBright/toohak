package com.example.notKahoot.ui.questions.list

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.notKahoot.R
import com.example.notKahoot.model.QuestionModel

class QuestionAdapter(private val onQuestionListener: OnQuestionListener) :
    RecyclerView.Adapter<QuestionAdapter.QuestionViewHolder>() {

    private var questionList = emptyList<QuestionModel>()

    class QuestionViewHolder(itemView: View, private val onQuestionListener: OnQuestionListener):
        RecyclerView.ViewHolder(itemView), View.OnClickListener {

        init {
            itemView.findViewById<CardView>(R.id.listView).setOnClickListener(this)
        }

        override fun onClick(view: View?) {
            onQuestionListener.onQuestionClick(absoluteAdapterPosition)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.custom_question, parent, false)
        return QuestionViewHolder(view, onQuestionListener)
    }

    override fun getItemCount(): Int {
        return questionList.size
    }

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        val currentItem = questionList[position]

        val optionQuestion = holder.itemView.findViewById<TextView>(R.id.questionTV)
        val optionOne = holder.itemView.findViewById<TextView>(R.id.option1)
        val optionTwo = holder.itemView.findViewById<TextView>(R.id.option2)
        val optionThree = holder.itemView.findViewById<TextView>(R.id.option3)
        val optionFour = holder.itemView.findViewById<TextView>(R.id.option4)

        currentItem.option_four = currentItem.correct_option

        val correct = "Correct:\n"
        val wrong = "Wrong:\n"
        // Cannot be replaced with string resources!
        optionQuestion.text = currentItem.question
        optionOne.text = wrong + currentItem.option_one
        optionTwo.text = wrong + currentItem.option_two
        optionThree.text = wrong + currentItem.option_three
        optionFour.text = correct + currentItem.option_four

        // If there is no value for options two and three
        if (currentItem.option_two.isEmpty() && currentItem.option_three.isEmpty()) {
            optionTwo.text = correct + currentItem.option_four
            optionThree.visibility = View.GONE
            optionFour.visibility = View.GONE
        } else if (currentItem.option_two.isEmpty()) {
            // If only option two is gone
            optionTwo.text = wrong + currentItem.option_three
            optionThree.text = correct + currentItem.option_four
            optionFour.visibility = View.GONE
        } else if (currentItem.option_three.isEmpty()) {
            // If only option three is gone
            optionThree.text = correct + currentItem.option_four
            optionFour.visibility = View.GONE
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(question: List<QuestionModel>) {
        this.questionList = question
        notifyDataSetChanged()
    }

    interface OnQuestionListener {
        fun onQuestionClick(position: Int)
    }

    private fun showToast(msg: String, context: Context) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
}