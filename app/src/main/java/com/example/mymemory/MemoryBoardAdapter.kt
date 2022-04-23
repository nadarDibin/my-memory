package com.example.mymemory

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.mymemory.models.GameBoard
import com.example.mymemory.models.MemoryCard
import com.squareup.picasso.Picasso
import kotlin.math.min

//    RecyclerView.Adapter<RecyclerView.ViewHolder>()
// MBA Class is a Sub-class of RV Adapter Class, parameterised by RV.VH
// VH is an Object that provides access to all the views of 1 RV element(1 Memory Piece in the Game)

class MemoryBoardAdapter(
    private val context: Context,
    private val gameBoard: GameBoard,
    private val cards: List<MemoryCard>,
    private val cardClickListener: CardClickListener
) :
/**                                Using custom viewHolder*/
        RecyclerView.Adapter<MemoryBoardAdapter.ViewHolder>() {

        /** COs are singletons where constants are defined. Members are accessible directly through the CC
         *  Like Static variables in Java */
        companion object {
            private const val MARGIN_SIZE = 10
            private const val TAG = "MemoryBoardAdapter"
        }

        interface CardClickListener {
            fun onCardClicked(position: Int) // Why bother?
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val cardWidth = parent.width / gameBoard.getWidth() - (2 * MARGIN_SIZE)
            val cardHeight = parent.height / gameBoard.getHeight() - (2 * MARGIN_SIZE)
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

        override fun getItemCount(): Int = this.gameBoard.numCards

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(position)
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val imageButton = itemView.findViewById<ImageButton>(R.id.imageButton)

            fun bind(position: Int) {
                val memoryCard = cards[position]
// TODO Refactor, remove if else ladder
                if (memoryCard.isFaceUp) {
                    if (memoryCard.imageUrl != null) { // TODO check ic_launcher_background
                        Picasso.get().load(memoryCard.imageUrl)
                            .placeholder(R.drawable.ic_loading_image).into(imageButton)
                    } else {
                        imageButton.setImageResource(memoryCard.identifier)
                    }
                } else {
                    imageButton.setImageResource(R.drawable.ic_launcher_background)
                }

                greyOutPair(imageButton, memoryCard)

                imageButton.setOnClickListener {
                    cardClickListener.onCardClicked(position)
                    Log.i(TAG, "Clicked on position: $position")
                }
            }
        }

        fun showCardFace(card: MemoryCard): Int {
            return if (card.isFaceUp) card.identifier else R.drawable.ic_launcher_background
        }

        fun greyOutPair(imageButton: ImageButton, memoryCard: MemoryCard) {
            imageButton.alpha = if (memoryCard.isMatched) .4f else 1.0f
            val colourStateLess = if (memoryCard.isMatched) ContextCompat.getColorStateList(
                context,
                R.color.color_gray
            ) else null
            ViewCompat.setBackgroundTintList(imageButton, colourStateLess)
        }
    }
