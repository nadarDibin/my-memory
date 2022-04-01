package com.example.mymemory

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mymemory.models.BoardSize

class MainActivity : AppCompatActivity() {

    /** lateinit because the values are set in the onCreate() and not when the class is constructed */
    private lateinit var rvBoard: RecyclerView
    private lateinit var tvNumMoves: TextView
    private lateinit var tvNumPairs: TextView

    private val boardSize: BoardSize = BoardSize.HARD

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvBoard = findViewById(R.id.rvBoard)
        tvNumMoves = findViewById(R.id.tvNumberOfMoves)
        tvNumPairs = findViewById(R.id.tvNumberOfPairs)

        rvBoard.adapter = MemoryBoardAdapter(this, boardSize)
        rvBoard.setHasFixedSize(true) // Optimisation
        rvBoard.layoutManager = GridLayoutManager(this, boardSize.getWidth())
        /**                                What is context exactly?  */
    }

    /**  RecyclerView has 2 components
     *     LayoutManager: measures and positions items
     *     Adapter: Takes underlying dataset of the RV and adapts each piece of data into a view
     */
}
