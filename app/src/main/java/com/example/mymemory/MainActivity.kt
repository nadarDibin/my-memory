package com.example.mymemory

import android.animation.ArgbEvaluator
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
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
import com.example.mymemory.models.UserImageList
import com.example.mymemory.utils.CUSTOM_BOARD_SIZE
import com.example.mymemory.utils.CUSTOM_GAME_NAME
import com.example.mymemory.utils.CreateActivity
import com.github.jinatonic.confetti.CommonConfetti
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso

class MainActivity : AppCompatActivity() {

    companion object {
        private const val CREATE_REQUEST_CODE = 1084 // Significance?
        private const val TAG = "MainActivity"
    }

    /** lateinit because the values are set in the onCreate() and not when the class is constructed */
    private lateinit var rvBoard: RecyclerView
    private lateinit var clRoot: ConstraintLayout
    private lateinit var tvNumMoves: TextView
    private lateinit var tvNumPairs: TextView

    private lateinit var memoryGame: MemoryGame
    private lateinit var adapter: MemoryBoardAdapter

    private val db = Firebase.firestore
    private var gameName: String? = null
    private var customGameImages: List<String>? = null
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
            R.id.mi_download -> {
                showDownloadDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CREATE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val customGameName = data?.getStringExtra(CUSTOM_GAME_NAME)
            if (customGameName == null) {
                Log.e(TAG, "Got null for custom game name in CreateActivity")
                return
            }
            downloadGame(customGameName)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun showDownloadDialog() {
        val downloadGameView =
            LayoutInflater.from(this).inflate(R.layout.dialog_download_game, null)
        showAlertDialog(
            "Download memory game", downloadGameView
        ) {
            val etDownloadGame = downloadGameView.findViewById<EditText>(R.id.etDownloadGame)
            val gameToDownload = etDownloadGame.text.toString().trim()
            downloadGame(gameToDownload)
        }
    }

    private fun downloadGame(customGameName: String) {
        db.collection("games").document(customGameName).get().addOnSuccessListener { document ->
            // we turn the document into a kotlin data class here using firestore
            val userImageList = document.toObject(UserImageList::class.java)
            if (userImageList?.images == null) {
                Log.e(TAG, "Invalid game data from Firestore")
                Snackbar.make(
                    clRoot,
                    "Sorry, game data for '$customGameName' not found",
                    Snackbar.LENGTH_SHORT
                ).show()
                return@addOnSuccessListener
            }

            val numCards = userImageList.images.size * 2
            gameBoard = GameBoard.getSizeByValue(numCards)
            customGameImages = userImageList.images
            gameName = customGameName
            // Pre-fetch the images for faster loading
            // TODO cache images?
            for (imageUrl in userImageList.images) {
                Picasso.get().load(imageUrl).fetch()
            }
            Snackbar.make(clRoot, "You are now playing '$customGameName'!", Snackbar.LENGTH_SHORT)
                .show()
            setupBoard()
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Exception while downloading custom game", exception)
        }
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
            // method to refresh data
            gameName = null
            customGameImages = null
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
                Snackbar.make(clRoot, "Think you can do better?", Snackbar.LENGTH_LONG)
            message.setAction("Play Again!") { setupBoard() }
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
        if (memoryGame.hasWonGame()) {
            val message =
                Snackbar.make(clRoot, "Congratulations! You Won!", Snackbar.LENGTH_LONG)
            message.show()
            CommonConfetti.rainingConfetti(
                clRoot,
                intArrayOf(Color.BLUE, Color.GREEN, Color.MAGENTA, Color.RED)
            ).oneShot()
            return
        }
    }

    private fun setupBoard() {

        supportActionBar?.title = gameName ?: getString(R.string.app_name)

        tvNumPairs.setTextColor(ContextCompat.getColor(this, R.color.color_progress_none))
        memoryGame = MemoryGame(gameBoard, customGameImages)
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
