package com.example.mymemory.utils

import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mymemory.ImagePickerAdapter
import com.example.mymemory.R
import com.example.mymemory.models.GameBoard

class CreateActivity : AppCompatActivity() {

    private lateinit var rvImagePicker: RecyclerView
    private lateinit var etGameName: EditText
    private lateinit var btnSave: Button

    private lateinit var board: GameBoard
    private var numImagesRequired = -1
    private val chosenImageUris = mutableListOf<Uri>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)

        rvImagePicker = findViewById(R.id.rvImagePicker)
        etGameName = findViewById(R.id.etGameName)
        btnSave = findViewById(R.id.btnSave)

        /** generates the Back button */
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        board = intent.getSerializableExtra(CUSTOM_BOARD_SIZE) as GameBoard
        numImagesRequired = board.getNumPairs()
        supportActionBar?.title = "Images selected (0/$numImagesRequired)"

        rvImagePicker.adapter = ImagePickerAdapter(this, chosenImageUris, board)
        rvImagePicker.setHasFixedSize(true)
        rvImagePicker.layoutManager = GridLayoutManager(this, board.getWidth())
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
