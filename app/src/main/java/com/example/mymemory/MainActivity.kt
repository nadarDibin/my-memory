package com.example.mymemory

import android.animation.ArgbEvaluator
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mymemory.models.GameBoard
import com.example.mymemory.models.MemoryGame
import com.example.mymemory.utils.CUSTOM_BOARD_SIZE
import com.example.mymemory.utils.CreateActivity
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    companion object {
        private const val CREATE_REQUEST_CODE = 1084 // Significance?
    }

    /** lateinit because the values are set in the onCreate() and not when the class is constructed */
    private lateinit var rvBoard: RecyclerView
    private lateinit var clRoot: ConstraintLayout
    private lateinit var tvNumMoves: TextView
    private lateinit var tvNumPairs: TextView

    private lateinit var memoryGame: MemoryGame
    private lateinit var adapter: MemoryBoardAdapter

    private var gameBoard: GameBoard = GameBoard.EASY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvBoard = findViewById(R.id.rvBoard)
        clRoot = findViewById(R.id.clRoot)
        tvNumMoves = findViewById(R.id.tvNumberOfMoves)
        tvNumPairs = findViewById(R.id.tvNumberOfPairs)

        setupBoard()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mi_refresh -> {
                setupBoard()
                return true // why return true?
            }
            R.id.mi_new_size -> {
                showNewSizeDialog()
                return true
            }
            R.id.mi_custom -> {
                showCreationDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showCreationDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioDifficulty)

        showAlertDialog(
            "Create your own board", boardSizeView
        ) {
            // set a new val for board size
            val customBoardSize = when (radioGroupSize.checkedRadioButtonId) {
                R.id.rbEasy -> GameBoard.EASY
                R.id.rbMedium -> GameBoard.MEDIUM
                else -> GameBoard.HARD
            }
            // navigate to new activity
            val intent = Intent(this, CreateActivity::class.java)
            intent.putExtra(CUSTOM_BOARD_SIZE, customBoardSize)
            startActivityForResult(intent, CREATE_REQUEST_CODE)
            setupBoard()
        }
    }

    private fun showNewSizeDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioDifficulty)
        when (gameBoard) {
            GameBoard.EASY -> radioGroupSize.check(R.id.rbEasy)
            GameBoard.MEDIUM -> radioGroupSize.check(R.id.rbMedium)
            GameBoard.HARD -> radioGroupSize.check(R.id.rbHard)
        }
        showAlertDialog(
            "Choose new difficulty", boardSizeView
        ) {
            gameBoard = when (radioGroupSize.checkedRadioButtonId) {
                R.id.rbEasy -> GameBoard.EASY
                R.id.rbMedium -> GameBoard.MEDIUM
                else -> GameBoard.HARD
            }
            setupBoard()
        }
    }

    private fun showAlertDialog(
        title: String,
        view: View?,
        positiveClickListener: View.OnClickListener
    ) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(view)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Ok") { _, _ -> positiveClickListener.onClick(null) }.show()
    }

    private fun updateGameWithFlip(position: Int) {
        if (memoryGame.hasWonGame()) {
            val message =
                Snackbar.make(clRoot, "Congratulations! You Won!", Snackbar.LENGTH_LONG)
            message.setAction("Play Again?") { setupBoard() }
            message.show()
            return
        }
        if (memoryGame.isCardFaceUp(position)) {
            Snackbar.make(clRoot, "Invalid move!", Snackbar.LENGTH_SHORT).show()
            return
        }
        if (memoryGame.makeMove(position)) {
            updateGameProgress()

            tvNumPairs.text =
                getString(R.string.pairs, memoryGame.numPairsFound, gameBoard.getNumPairs())
        }
        tvNumMoves.text = getString(R.string.moves, memoryGame.getGameMoves())

        /** IMPORTANT */
        adapter.notifyItemRangeChanged(0, memoryGame.cards.size, memoryGame)
    }

    private fun updateGameProgress() {
        val progressColour = ArgbEvaluator().evaluate(
            memoryGame.numPairsFound.toFloat() / gameBoard.getNumPairs(),
            ContextCompat.getColor(this, R.color.color_progress_none),
            ContextCompat.getColor(this, R.color.color_progress_full)
        ) as Int
        tvNumPairs.setTextColor(progressColour)
    }

    private fun setupBoard() {

        tvNumPairs.setTextColor(ContextCompat.getColor(this, R.color.color_progress_none))
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

        tvNumPairs.text =
            getString(R.string.pairs, memoryGame.numPairsFound, gameBoard.getNumPairs())
        tvNumMoves.text = getString(R.string.moves, memoryGame.getGameMoves())
    }

    /**  RecyclerView has 2 components
     *     LayoutManager: measures and positions items
     *     Adapter: Takes underlying dataset of the RV and adapts each piece of data into a view
     */
}
