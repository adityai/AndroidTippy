package com.example.tippy

import android.animation.ArgbEvaluator
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

private const val TAG = "MainActivity"
private const val INITIAL_TIP_PERCENT = 15

class MainActivity : AppCompatActivity() {
    private lateinit var editableTextBaseAmount: EditText
    private lateinit var seekbarTipPercentage: SeekBar
    private lateinit var textViewTipPercentage: TextView
    private lateinit var textTipAmount: TextView
    private lateinit var textTotalAmount: TextView
    private lateinit var textViewTipDescription: TextView
    private lateinit var textViewFunnyMessage: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tippy()
    }

    private fun tippy() {
        editableTextBaseAmount = findViewById(R.id.editableTextBaseAmount)
        seekbarTipPercentage = findViewById(R.id.seekBarTipPercentage)
        textViewTipPercentage = findViewById(R.id.textViewTipPercentage)
        textTipAmount = findViewById(R.id.textTipAmount)
        textTotalAmount = findViewById(R.id.textTotalAmount)
        textViewTipDescription = findViewById(R.id.textViewTipDescription)
        textViewFunnyMessage = findViewById(R.id.textViewFunnyMessage)

        seekbarTipPercentage.progress = INITIAL_TIP_PERCENT
        textViewTipPercentage.text = "$INITIAL_TIP_PERCENT%"
        updateTipDescription(INITIAL_TIP_PERCENT)
        seekbarTipPercentage.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                Log.i(TAG, "onProgressChanged $progress")
                textViewTipPercentage.text = "$progress%"
                computeTipAndTotal()
                updateTipDescription(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        editableTextBaseAmount.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                Log.i(TAG, "afterTextChanged $s")
                computeTipAndTotal()
            }

        })
        displayFunnyMessage()
    }

    // function to display a random funny message about tips
    private fun displayFunnyMessage() {
        val funnyMessages = listOf(
            "You are a good tipper",
            "You are a great tipper",
            "You are a wonderful tipper",
            "You are a master tipper",
            "You are a legendary tipper"
        )
        val randomFunnyMessage = funnyMessages.random()
        textViewFunnyMessage.text = randomFunnyMessage
    }

    private fun updateTipDescription(tipPercentage: Int) {
        val tipDescription = when(tipPercentage) {
            in 0..9 -> "It is not that bad is it?"
            in 10..14 -> "A bit low, isn't it?"
            in 15..19 -> "Good"
            in 20..24 -> "Great"
            else -> "Amazing"
        }
        textViewTipDescription.text = tipDescription
        // Update the color based on tipPercent using interpolation
        val color = ArgbEvaluator().evaluate(
            tipPercentage.toFloat() / seekbarTipPercentage.max,
            ContextCompat.getColor(this, R.color.color_worst_tip),
            ContextCompat.getColor(this, R.color.color_best_tip)
        ) as Int
        textViewTipDescription.setTextColor(color)
        displayFunnyMessage()
    }

    private fun computeTipAndTotal() {
        if (editableTextBaseAmount.text.isEmpty()) {
            textTipAmount.text = ""
            textTotalAmount.text = ""
        } else {
            val baseAmount = editableTextBaseAmount.text.toString().toDouble()
            val tipPercent = seekbarTipPercentage.progress
            val tipAmount = (baseAmount * tipPercent / 100)
            val totalAmount = baseAmount + tipAmount
            textTipAmount.text = "%.2f".format(tipAmount)
            textTotalAmount.text = "%.2f".format(totalAmount)
        }
    }
}