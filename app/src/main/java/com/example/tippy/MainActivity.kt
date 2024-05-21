package com.example.tippy

import android.animation.ArgbEvaluator
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.room.*
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import javax.net.ssl.*

@Database(entities = [PhotoEntity::class], version = 1)
abstract class PhotoDatabase : RoomDatabase() {
    abstract fun photoDao(): PhotoDao
}

@Dao
interface PhotoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: PhotoEntity)

    @Query("SELECT * FROM photos")
    suspend fun getAllPhotos(): List<PhotoEntity>
}

@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey val imgSrc: String,
    val earthDate: String
)


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

    companion object {
        lateinit var database: PhotoDatabase
        var savedPhotoIndex: Int = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = Room.databaseBuilder(
            applicationContext,
            PhotoDatabase::class.java, "photo-database"
        ).build()
        ApiKeyActivity.database = Room.databaseBuilder(
            applicationContext,
            ApiKeyDatabase::class.java, "apikey-database"
        ).build()
        setContentView(R.layout.activity_main)

        val setApiKeyButton = findViewById<Button>(R.id.setApiKeyButton)
        setApiKeyButton.setOnClickListener {
            val intent = Intent(this, ApiKeyActivity::class.java)
            startActivity(intent)
        }

        val showSlideShowButton = findViewById<Button>(R.id.showSlideShowButton)
        showSlideShowButton.setOnClickListener {
            val intent = Intent(this, SlideShowActivity::class.java)
            startActivity(intent)
        }

        curiosity()
        tippy()
    }

    // Function to get a random date in yyyy-mm-dd format
    private fun randomDate(): String {
        val year = (2014..2024).random()
        val month = (1..12).random()
        val day = (1..28).random()
        return "$year-$month-$day"
    }

    private fun getApiKey(): String {
        return runBlocking {
            withContext(Dispatchers.IO) {
                val apiKeys = ApiKeyActivity.database.apiKeyDao().getApiKey()
                if (apiKeys.isNotEmpty()) {
                    apiKeys[0].apikey
                } else {
                    "" // Return an empty string or handle the case when no API keys are found
                }
            }
        }
    }

    private fun curiosity() {
//        val apiKey = "5M12ifePfRKP7c9ywgRFXLYq5J8JHasG8zOKaect"
        var apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            apiKey = "DEMO_KEY"
        }
        //TODO: If apiKey is DEMO_KEY, display a message to the user that they need to set an API key
        //TODO: If apiKey is DEMO_KEY, allow the user to click on the AnotherEarthDate button three times and then stop showing the curiosity image
        val earthDate = randomDate()

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
                    if (photos.isNotEmpty()) {
                        val randomPhoto = photos.random()
                        Log.d("Photo", "ID: ${randomPhoto.id}, URL: ${randomPhoto.img_src}, Date: ${randomPhoto.earth_date}")

                        // Run everything UI related on the main thread
                        runOnUiThread {
                            val imageView = findViewById<ImageView>(R.id.curiosityImageView)
                            val earthDateTextView = findViewById<TextView>(R.id.earthDateTextView)

                            // Set OnClickListener to display a random image when curiosityImageView is clicked
                            imageView.setOnClickListener {
                                val newRandomPhoto = photos.random()
                                Picasso.get().load(newRandomPhoto.img_src).into(imageView)
                                earthDateTextView.text = newRandomPhoto.earth_date
                                imageView.contentDescription = newRandomPhoto.img_src.toString()
                            }

                            // Initially, display a random image
                            Picasso.get().load(randomPhoto.img_src).into(imageView)
                            earthDateTextView.text = randomPhoto.earth_date
                            imageView.contentDescription = randomPhoto.img_src.toString()
                        }
                    } else {
                        Log.i("Curiosity:", "No photos found. Retrying...")
                        curiosity()
                    }
                } else {
                    Log.e("API Call", "Failed: ${response.code}")
                }

//                val setApiKeyButton = findViewById<Button>(R.id.setApiKeyButton)
//                setApiKeyButton.setOnClickListener {
//                    val intent = Intent(this, ApiKeyActivity::class.java)
//                    startActivity(intent)
//                }
            }
        })

        // Set OnClickListener on anotherEarthDateButton to display another earth date image
        val anotherEarthDateButton = findViewById<Button>(R.id.anotherEarthDateButton)
        anotherEarthDateButton.setOnClickListener {
            Log.i("Another Earth Date:", "Button clicked")
            curiosity()
        }

        // Set OnClickListener on saveButton to save the earth date and id of the image currently displayed on the screen
        val saveButton = findViewById<Button>(R.id.saveButton)
        saveButton.setOnClickListener {
            val imageView = findViewById<ImageView>(R.id.curiosityImageView)
            val earthDateTextView = findViewById<TextView>(R.id.earthDateTextView)
            val earthDate = earthDateTextView.text.toString()
            val imgSrc = imageView.contentDescription.toString()
            Log.d("Save Photo Info:", "ID: $imgSrc, Date: $earthDate")
            val photoEntity = PhotoEntity(imgSrc = "$imgSrc", earthDate = "$earthDate")
            runBlocking {
                launch(Dispatchers.IO) {
                    MainActivity.database.photoDao().insert(photoEntity)
                    val allPhotos = MainActivity.database.photoDao().getAllPhotos()
                    for (photo in allPhotos) {
                        Log.d("Photo", "imgSrc: ${photo.imgSrc}, Date: ${photo.earthDate}")
                    }
                }
            }
        }

        val showSavedButton = findViewById<Button>(R.id.showSavedButton)
        showSavedButton.setOnClickListener {
            runBlocking {
                launch(Dispatchers.IO) {
                    val allSavedPhotos = MainActivity.database.photoDao().getAllPhotos()
                    val allSavedPhotosSize = allSavedPhotos.size
                    if (allSavedPhotosSize > 0) {
                        Log.i("Saved Photos:", "There are ${allSavedPhotosSize} saved photos.")
                        Log.d("Photo", "imgSrc: ${allSavedPhotos[savedPhotoIndex].imgSrc}, Date: ${allSavedPhotos[savedPhotoIndex].earthDate}")
                        // Switch to the main thread for UI operations
                        runOnUiThread {
                            val imageView = findViewById<ImageView>(R.id.curiosityImageView)
                            val earthDateTextView = findViewById<TextView>(R.id.earthDateTextView)
                            imageView.contentDescription = allSavedPhotos[savedPhotoIndex].imgSrc.toString()
                            Picasso.get().load(allSavedPhotos[savedPhotoIndex].imgSrc).into(imageView)
                            earthDateTextView.text = allSavedPhotos[savedPhotoIndex].earthDate.toString()

                            if (savedPhotoIndex + 1 == allSavedPhotosSize) {
                                // Reset the index if it exceeds the size of the list
                                savedPhotoIndex = 0
                            } else {
                                savedPhotoIndex++
                            }
                        }
                    }
                    else {
                        Log.i("Saved Photos:", "There are no saved photos.")
                    }

                }
            }
        }
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