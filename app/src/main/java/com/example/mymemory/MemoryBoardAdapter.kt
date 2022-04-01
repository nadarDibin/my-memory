package com.example.mymemory

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.min

//    RecyclerView.Adapter<RecyclerView.ViewHolder>()
// MBA Class is a Sub-class of RV Adapter Class, parameterised by RV.VH
// VH is an Object that provides access to all the views of 1 RV element(1 Memory Piece in the Game)

class MemoryBoardAdapter(private val context: Context, private val numMemoryPieces: Int) :
/**                                Using custom viewHolder*/
        RecyclerView.Adapter<MemoryBoardAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val cardWidth = parent.width / 2
            val cardHeight = parent.height / 4
            val cardSideLength = min(cardHeight, cardWidth)
            val view = LayoutInflater.from(context).inflate(
                R.layout.memory_card,
                parent,
                false
            ) // inflate returns the actual view which was created

            val layoutParams = view.findViewById<CardView>(R.id.cardView).layoutParams
            layoutParams.width = cardSideLength
            layoutParams.height = cardSideLength

            return ViewHolder(view)
        }

        override fun getItemCount(): Int = this.numMemoryPieces

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(position)
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(position: Int) {
//                TODO("Not yet implemented")
            }
        }
    }
