package com.example.notKahoot.ui.createGame.playGame


import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.notKahoot.R
import java.util.*

class ResultAdapter(
    private val results: List<GameplayQuizFragment.QuizResultsPayload.IndividualResults>?,
    private val selfUuid: UUID
)
    : RecyclerView.Adapter<ResultAdapter.ViewHolder>()
{
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val container: CardView
        val rankingView: TextView
        val nameView: TextView
        val subheadingView: TextView

        init {
            container = view.findViewById(R.id.listView)
            rankingView = view.findViewById(R.id.resultRanking)
            nameView = view.findViewById(R.id.resultName)
            subheadingView = view.findViewById(R.id.resultSubheading)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
        {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.custom_result_row, parent, false)
            return ViewHolder(view)
        }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val context = viewHolder.container.context

        val result = results?.get(position)
        viewHolder.rankingView.text = "#${position+1}"
        viewHolder.nameView.text = result?.name ?: context.getString(R.string.unknown_name)
        viewHolder.subheadingView.text = result?.let { "${it.numCorrect}/${it.numAnswered}" } ?: "0/0"


        viewHolder.container.background = ContextCompat.getDrawable(
            context,
            if (result?.id == selfUuid) R.drawable.current_player_result_border else
                R.drawable.default_result_border
        )
    }

    override fun getItemCount(): Int = results?.size?: 0
}
