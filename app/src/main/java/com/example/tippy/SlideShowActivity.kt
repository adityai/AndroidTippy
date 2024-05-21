package com.example.tippy

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.room.Room
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class SlideShowActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var dateTextView: TextView
    private var photos = listOf<PhotoEntity>()
    private var currentIndex = 0
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_slide_show)
        val endSlideShowButton = findViewById<Button>(R.id.endSlideShowButton)
        imageView = findViewById<ImageView>(R.id.slideShowImageView)
        dateTextView = findViewById<TextView>(R.id.photoDateTextView)

        MainActivity.database = Room.databaseBuilder(
            applicationContext,
            PhotoDatabase::class.java, "photo-database"
        ).build()

        endSlideShowButton.setOnClickListener {
            finish()
        }
        loadPhotos()
    }

    private fun loadPhotos() {
        runBlocking {
            withContext(Dispatchers.IO) {
                photos = MainActivity.database.photoDao().getAllPhotos()
            }

            if (photos.isNotEmpty()) {
                startSlideShow()
            } else {
                Toast.makeText(this@SlideShowActivity, "No photos found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startSlideShow() {
        handler = Handler(Looper.getMainLooper())
        runnable = object : Runnable {
            override fun run() {
                displayPhoto(photos[currentIndex])
                currentIndex = (currentIndex + 1) % photos.size
                handler.postDelayed(this, 10000) // 10 seconds delay
            }
        }
        handler.post(runnable)
    }

    private fun displayPhoto(photo: PhotoEntity) {
        Picasso.get().load(photo.imgSrc).into(imageView)
        dateTextView.text = photo.earthDate
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable) // Stop the slideshow when activity is destroyed
    }
}