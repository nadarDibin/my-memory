package com.example.mymemory.utils

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.example.mymemory.R
import com.example.mymemory.models.GameBoard

class CreateActivity : AppCompatActivity() {

    private lateinit var board: GameBoard
    private var numImagesRequired = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)

        /** generates the Back button */
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        board = intent.getSerializableExtra(CUSTOM_BOARD_SIZE) as GameBoard
        numImagesRequired = board.getNumPairs()
        supportActionBar?.title = "Images selected (0/$numImagesRequired)"
    }

    /** Dictates the behaviour of the Back button */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
