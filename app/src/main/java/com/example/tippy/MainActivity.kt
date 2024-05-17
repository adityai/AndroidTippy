package com.example.tippy

import android.animation.ArgbEvaluator
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.squareup.picasso.Picasso
import okhttp3.OkHttpClient
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import javax.net.ssl.*

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
        curiosity()
        tippy()
    }

    private fun curiosity() {
        val apiKey = "5M12ifePfRKP7c9ywgRFXLYq5J8JHasG8zOKaect"
        val earthDate = "2014-01-31"

        val url = "https://api.nasa.gov/mars-photos/api/v1/rovers/curiosity/photos?api_key=$apiKey&earth_date=$earthDate"

        val client = getUnsafeOkHttpClient()

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("API Call", "Failed: ${e.message}")
            }
//                        Log.d("Photo", "ID: ${it.id}, URL: ${it.imgSrc}, Date: ${it.earthDate}")

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val jsonData = response.body?.string()
                    val photos = parseJson(jsonData)
                    // Get a random photo from the list of photos
                    val randomPhoto = photos.random()
                    Log.d("Photo", "ID: ${randomPhoto.id}, URL: ${randomPhoto.img_src}, Date: ${randomPhoto.earth_date}")
                    val imageView = findViewById<ImageView>(R.id.curiosityImageView)
                    runOnUiThread { Picasso.get().load(randomPhoto.img_src).into(imageView) }

//                    Log.d("Photo", "ID: ${photos[0].id}, URL: ${photos[0].img_src}, Date: ${photos[0].earth_date}")
//                    val imageView = findViewById<ImageView>(R.id.curiosityImageView)
//                    runOnUiThread {
//                        Picasso.get().load(photos[0].img_src).into(imageView)
//                    }
                } else {
                    Log.e("API Call", "Failed: ${response.code}")
                }
            }
        })
    }

    data class Photo(
        val id: Int,
        val img_src: String,
        val earth_date: String
    )


    private fun parseJson(jsonData: String?): List<Photo> {
        val photos = mutableListOf<Photo>()
        val jsonObject = JSONObject(jsonData)
        val jsonArray = jsonObject.getJSONArray("photos")
        for (i in 0 until jsonArray.length()) {
            val photoJson = jsonArray.getJSONObject(i)
            val id = photoJson.getInt("id")
            val imgSrc = photoJson.getString("img_src")
            val earthDate = photoJson.getString("earth_date")
            photos.add(Photo(id, imgSrc, earthDate))
        }
        return photos
    }

    private fun getUnsafeOkHttpClient(): OkHttpClient {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {
                }

                override fun checkServerTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {
                }

                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> {
                    return arrayOf()
                }
            })

            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())

            val sslSocketFactory = sslContext.socketFactory

            return OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .build()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
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