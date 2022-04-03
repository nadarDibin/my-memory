package com.example.mymemory

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mymemory.models.GameBoard
import com.example.mymemory.models.MemoryGame
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "Main Activity"
    }

    /** lateinit because the values are set in the onCreate() and not when the class is constructed */
    private lateinit var rvBoard: RecyclerView
    private lateinit var clRoot: ConstraintLayout
    private lateinit var tvNumMoves: TextView
    private lateinit var tvNumPairs: TextView

    private lateinit var memoryGame: MemoryGame
    private lateinit var adapter: MemoryBoardAdapter

    private val gameBoard: GameBoard = GameBoard.HARD

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvBoard = findViewById(R.id.rvBoard)
        clRoot = findViewById(R.id.clRoot)
        tvNumMoves = findViewById(R.id.tvNumberOfMoves)
        tvNumPairs = findViewById(R.id.tvNumberOfPairs)

        memoryGame = MemoryGame(gameBoard)

        adapter = MemoryBoardAdapter(
            this,
            gameBoard,
            memoryGame.cards,
            object : MemoryBoardAdapter.CardClickListener {
                /** Creating an anonymous class of CardClickListener */
                override fun onCardClicked(position: Int) {
                    updateGameWithFlip(position)
                }
            }
        )
        rvBoard.adapter = adapter
        rvBoard.setHasFixedSize(true) // Optimisation
        rvBoard.layoutManager = GridLayoutManager(this, gameBoard.getWidth())
        /**                                What is context exactly?  */
    }

    private fun updateGameWithFlip(position: Int) {
        if (memoryGame.hasWonGame()) {
            val message =
                Snackbar.make(clRoot, "Congratulations! You Won!", Snackbar.LENGTH_LONG)
            message.setAction("Play Again?") { restartGame() }
            message.show()
            return
        }
        if (memoryGame.isCardFaceUp(position)) {
            Snackbar.make(clRoot, "Invalid move!", Snackbar.LENGTH_SHORT).show()
            return
        }
        memoryGame.makeMove(position)
        /** IMPORTANT */
        adapter.notifyDataSetChanged()
    }

    private fun restartGame() {
        /**  What is intent?*/
        val intent = intent
        finish()
        startActivity(intent)
    }

    /**  RecyclerView has 2 components
     *     LayoutManager: measures and positions items
     *     Adapter: Takes underlying dataset of the RV and adapts each piece of data into a view
     */
}
