package com.example.mymemory

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.mymemory.models.BoardSize
import kotlin.math.min

//    RecyclerView.Adapter<RecyclerView.ViewHolder>()
// MBA Class is a Sub-class of RV Adapter Class, parameterised by RV.VH
// VH is an Object that provides access to all the views of 1 RV element(1 Memory Piece in the Game)

class MemoryBoardAdapter(
    private val context: Context,
    private val boardSize: BoardSize,
    private val cardImages: List<Int>
) :
/**                                Using custom viewHolder*/
        RecyclerView.Adapter<MemoryBoardAdapter.ViewHolder>() {

        /** COs are singletons where constants are defined. Members are accessible directly through the CC
         *  Like Static variables in Java */
        companion object {
            private const val MARGIN_SIZE = 10
            private const val TAG = "MemoryBoardAdapter"
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val cardWidth = parent.width / boardSize.getWidth() - (2 * MARGIN_SIZE)
            val cardHeight = parent.height / boardSize.getHeight() - (2 * MARGIN_SIZE)
            val cardSideLength = min(cardHeight, cardWidth)
            val view = LayoutInflater.from(context).inflate(
                R.layout.memory_card,
                parent,
                false
            ) // inflate returns the actual view which was created

            val layoutParams =
                view.findViewById<CardView>(R.id.cardView).layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.width = cardSideLength
            layoutParams.height = cardSideLength
            layoutParams.setMargins(MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE)

            return ViewHolder(view)
        }

        override fun getItemCount(): Int = this.boardSize.numCards

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(position)
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val imageButton = itemView.findViewById<ImageButton>(R.id.imageButton)

            fun bind(position: Int) {
                imageButton.setImageResource(cardImages[position])
                imageButton.setOnClickListener {
                    Log.i(TAG, "Clicked on position: $position")
                }
            }
        }
    }
