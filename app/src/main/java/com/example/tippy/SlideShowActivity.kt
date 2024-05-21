package com.example.tippy

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class SlideShowActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_slide_show)
        val endSlideShowButton = findViewById<Button>(R.id.endSlideShowButton)

        endSlideShowButton.setOnClickListener {
            finish()
        }

    }
}