package com.example.mymemory.utils

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mymemory.ImagePickerAdapter
import com.example.mymemory.R
import com.example.mymemory.models.GameBoard
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.ByteArrayOutputStream

class CreateActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CreateActivity"
        private const val PICK_PHOTO_CODE = 1123 // use Enums maybe, do better
        private const val READ_EXTERNAL_PHOTOS_CODE = 4801
        private const val READ_PHOTOS_PERMISSION = android.Manifest.permission.READ_EXTERNAL_STORAGE
        private const val MAX_GAME_NAME_LENGTH = 14
    }

    private lateinit var rvImagePicker: RecyclerView
    private lateinit var etGameName: EditText
    private lateinit var btnSave: Button
    private lateinit var pbUploading: ProgressBar

    private lateinit var adapter: ImagePickerAdapter
    private lateinit var board: GameBoard
    private var numImagesRequired = -1
    private val chosenImageUris = mutableListOf<Uri>()

    private val storage = Firebase.storage
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)

        rvImagePicker = findViewById(R.id.rvImagePicker)
        etGameName = findViewById(R.id.etGameName)
        btnSave = findViewById(R.id.btnSave)
        pbUploading = findViewById(R.id.pbUploading)

        /** generates the Back button */
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        board = intent.getSerializableExtra(CUSTOM_BOARD_SIZE) as GameBoard
        numImagesRequired = board.getNumPairs()
        supportActionBar?.title = "Images selected (0/$numImagesRequired)"

        btnSave.setOnClickListener {
            saveDataToFirebase()
        }

        setGameNameLength()
        etGameName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(p0: Editable?) {
                btnSave.isEnabled = shouldEnableSaveButton()
            }
        })

        adapter = ImagePickerAdapter(
            this,
            chosenImageUris,
            board,
            object : ImagePickerAdapter.ImageClickListener {
                override fun onPlaceholderClicked() {
                    if (isPermissionGranted(this@CreateActivity, READ_PHOTOS_PERMISSION)) {
                        launchIntentForPhotos()
                    } else {
                        requestPermission(
                            this@CreateActivity,
                            READ_PHOTOS_PERMISSION,
                            READ_EXTERNAL_PHOTOS_CODE
                        )
                    }
                }
            }
        )
        rvImagePicker.adapter = adapter
        rvImagePicker.setHasFixedSize(true)
        rvImagePicker.layoutManager = GridLayoutManager(this, board.getWidth())
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_activity_create, menu)
        return true
    }

    /** Dictates the behaviour of the Back button */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mi_refresh -> {
                // TODO: relaunch screen
                return true
            }
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == READ_EXTERNAL_PHOTOS_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchIntentForPhotos()
            } else {
                Toast.makeText(
                    this,
                    "To create a custom game please provide access to your photos",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != PICK_PHOTO_CODE || resultCode != Activity.RESULT_OK || data == null) {
            Log.w(TAG, "Did not get data from app launched, user likely changed their mind")
            return
        }
        val selectedUri = data.data
        val clipData = data.clipData // what is clipdata
        if (clipData != null) {
            Log.i(TAG, "clipData numImages ${clipData.itemCount}: $clipData")
            for (i in 0 until clipData.itemCount) {
                val clipItem = clipData.getItemAt(i)
                if (chosenImageUris.size < numImagesRequired) {
                    chosenImageUris.add(clipItem.uri)
                }
            }
        } else if (selectedUri != null) {
            Log.i(TAG, "data: $selectedUri")
            chosenImageUris.add(selectedUri)
        }
        adapter.notifyDataSetChanged()
        supportActionBar?.title = "Images selected (${chosenImageUris.size}/$numImagesRequired)"
        btnSave.isEnabled = shouldEnableSaveButton()
    }

    private fun saveDataToFirebase() {
        Log.i(TAG, "saveDataToFirebase")
        btnSave.isEnabled = false
        val gameName = etGameName.text.toString()

        db.collection("games").document(gameName).get().addOnSuccessListener { document ->
            if (document != null && document.data != null) { // better way?
                AlertDialog.Builder(this)
                    .setTitle("Name taken")
                    .setMessage("A game with the name $gameName already exists, Please choose another name")
                    .setPositiveButton("Okay", null)
                    .show()
                btnSave.isEnabled = true
            } else {
                handleImageUploading(gameName)
            }
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Encountered error while saving the game", exception)
            Toast.makeText(this, "Encountered error while saving the game", Toast.LENGTH_SHORT)
                .show()
            btnSave.isEnabled = true
        }
    }

    private fun handleImageUploading(gameName: String) {
        pbUploading.visibility = View.VISIBLE
        var didEncounterError = false
        val uploadedImageUrls = mutableListOf<String>()
        for ((index, photoUri) in chosenImageUris.withIndex()) {
            val imageByteArray = getImageByteArray(photoUri) // actual image data
            val filePath = "game_images/$gameName/${System.currentTimeMillis()}-$index.jpg"
            val photoLocationReference = storage.reference.child(filePath)

            /** uploads data to firebase (Expensive) */
            photoLocationReference.putBytes(imageByteArray)
                .continueWithTask { photoUploadTask -> // photoUploadTask is the result of putBytes
                    Log.i(TAG, "Uploaded bytes: ${photoUploadTask.result?.bytesTransferred}")
                    photoLocationReference.downloadUrl // if the Second task
                }.addOnCompleteListener { downloadUrlTask -> // downloadUrlTask is from .downloadUrl
                    if (didEncounterError) {
                        pbUploading.visibility = View.GONE
                        return@addOnCompleteListener
                    } // for any earlier image
                    if (!downloadUrlTask.isSuccessful) {
                        Log.e(TAG, "Exception with Firebase Storage", downloadUrlTask.exception)
                        Toast.makeText(this, "Failed to upload image!", Toast.LENGTH_SHORT).show()
                        didEncounterError = true // for this image
                        pbUploading.visibility = View.GONE
                        return@addOnCompleteListener
                    }
                    val downloadUrl = downloadUrlTask.result.toString()
                    uploadedImageUrls.add(downloadUrl)
                    pbUploading.progress = uploadedImageUrls.size * 100 / chosenImageUris.size
                    Log.i(
                        TAG,
                        "Finished uploading (${uploadedImageUrls.size}/${chosenImageUris.size}) images"
                    )
                    if (uploadedImageUrls.size == chosenImageUris.size) {
                        handleAllImagesUploaded(gameName, uploadedImageUrls)
                    }
                }
        }
    }

    private fun handleAllImagesUploaded(gameName: String, imageUrls: MutableList<String>) {

        // Firestore has data in documents which has it in collection, 1 document is an entity
        db.collection("games").document(gameName)
            .set(mapOf("images" to imageUrls))
            .addOnCompleteListener { gameCreationTask ->
                pbUploading.visibility = View.GONE
                if (!gameCreationTask.isSuccessful) {
                    Log.e(TAG, "Exception in game creation", gameCreationTask.exception)
                    Toast.makeText(this, "Game creation failed", Toast.LENGTH_SHORT).show()
                    return@addOnCompleteListener
                }
                Log.i(TAG, "Successfully created the game: $gameName")
                AlertDialog.Builder(this)
                    .setTitle("Upload complete! Play '$gameName'?")
                    .setPositiveButton("Yes!") { _, _ ->
                        val resultData = Intent()
                        resultData.putExtra(CUSTOM_GAME_NAME, gameName)
                        setResult(Activity.RESULT_OK, resultData)
                        finish()
                    }
                    .setNegativeButton("No") { _, _ ->
                        // TODO implement no to take you back to the default game
                    }
                    .show()
            }
    }

    private fun getImageByteArray(photoUri: Uri): ByteArray {
        // is this needed?
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, photoUri)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(contentResolver, photoUri)
        }

        val byteOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteOutputStream)
        return byteOutputStream.toByteArray()
    }

    private fun setGameNameLength() {
        etGameName.filters = arrayOf(InputFilter.LengthFilter(MAX_GAME_NAME_LENGTH))
    }

    private fun shouldEnableSaveButton(): Boolean {
        if (chosenImageUris.size != numImagesRequired) return false
        if (isTextEmpty()) return false
        return true
    }

    private fun isTextEmpty() = etGameName.text.toString().isEmpty()

    private fun launchIntentForPhotos() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(
            Intent.createChooser(intent, "Selected desired pictures"),
            PICK_PHOTO_CODE
        )
    }
}
