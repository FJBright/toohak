package com.example.notKahoot.ui.createGame.quizSelect

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.notKahoot.R
import com.example.notKahoot.model.QuizModel

class AdapterQuizSelect(private val onQuizListener: OnQuizListener) :
    RecyclerView.Adapter<AdapterQuizSelect.QuizViewHolder>() {

    private var quizList = emptyList<QuizModel>()

    class QuizViewHolder(itemView: View, private val onQuizListener: OnQuizListener):
        RecyclerView.ViewHolder(itemView), View.OnClickListener {

        init {
            itemView.findViewById<CardView>(R.id.listView).setOnClickListener(this)
        }

        override fun onClick(view: View?) {
            onQuizListener.onQuizClick(absoluteAdapterPosition)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuizViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.custom_row, parent, false)
        return QuizViewHolder(view, onQuizListener)
    }

    override fun getItemCount(): Int {
        return quizList.size
    }

    override fun onBindViewHolder(holder: QuizViewHolder, position: Int) {
        val currentItem = quizList[position]

        holder.itemView.findViewById<TextView>(R.id.quizTitleTV).text = currentItem.quizTitle

//        holder.itemView.findViewById<ConstraintLayout>(R.id.rowLayout).setOnClickListener {
//            val options = arrayOf("Play", "View Questions", "Edit/Delete")
//            val builder = AlertDialog.Builder(holder.itemView.context)
//            builder.setTitle("Manage Quiz")
//            builder.setItems(options) { _, optionId ->
//                quizManagementOptions(optionId, currentItem, holder.itemView)
//            }
//            builder.show()
//        }
    }

    /**
     * Handles the interaction of a single recyclerView quiz item.
     */
//    private fun quizManagementOptions(optionId: Int, currentItem: QuizModel, itemView: View) {
//        when (optionId) {
//            0 -> {
//                // Examples of navigation with args defined in the navigation graph xml
//                val action = FragmentQuizListDirections.actionNavigationQuizListToCreateGameFragment(currentItem)
//                itemView.findNavController().navigate(action)
//            }
//            1 -> {
//                val action = FragmentQuizListDirections.actionNavigationQuizListToNavigationQuestionList(currentItem)
//                itemView.findNavController().navigate(action)
//            }
//            2 -> {
//                val action = FragmentQuizListDirections.actionListFragmentToUpdateFragment(currentItem)
//                itemView.findNavController().navigate(action)
//            }
//        }
//    }
//    private fun showToast(msg: String, context: Context) {
//        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
//    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(quiz: List<QuizModel>) {
        this.quizList = quiz
        notifyDataSetChanged()
    }

    interface OnQuizListener {
        fun onQuizClick(position: Int)
    }
}