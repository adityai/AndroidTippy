package com.example.tippy

import android.widget.EditText
import android.os.Bundle
import android.text.Editable
import android.text.method.LinkMovementMethod
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.room.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ApiKeyActivity : AppCompatActivity() {

    companion object {
        lateinit var database: ApiKeyDatabase
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = Room.databaseBuilder(
            applicationContext,
            ApiKeyDatabase::class.java, "apikey-database"
        ).build()
        setContentView(R.layout.activity_api_key)

        val instructionsTextView = findViewById<TextView>(R.id.instructionsTextView)
        instructionsTextView.movementMethod = LinkMovementMethod.getInstance()
        val saveButton = findViewById<Button>(R.id.saveApiKeyButton)
        val cancelButton = findViewById<Button>(R.id.cancelApiKeyButton)
        val apiKeyEditText = findViewById<EditText>(R.id.apiKeyEditText)

        runBlocking {
            launch(Dispatchers.IO) {
                val apiKeys = ApiKeyActivity.database.apiKeyDao().getApiKey()
                val apiKeyString = apiKeys[0].apikey

                runOnUiThread {
                    apiKeyEditText.text = Editable.Factory.getInstance().newEditable(apiKeyString)
                }
            }
        }

        cancelButton.setOnClickListener {
            finish()
        }

        saveButton.setOnClickListener {
            val apiKey = apiKeyEditText.text.toString()

            runBlocking {
                launch(Dispatchers.IO) {
                    ApiKeyActivity.database.apiKeyDao().deleteAllApiKeys()
                    ApiKeyActivity.database.apiKeyDao().insert(ApiKeyEntity(apiKey))
                }
            }
            finish()
        }
    }
}

@Database(entities = [ApiKeyEntity::class], version = 1)
abstract class ApiKeyDatabase : RoomDatabase() {
    abstract fun apiKeyDao(): ApiKeyDao
}

@Entity(tableName = "apikeytable")
data class ApiKeyEntity(
    @PrimaryKey val apikey: String
)

@Dao
interface ApiKeyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(apikeytable: ApiKeyEntity)

    @Query("SELECT apikey FROM apikeytable")
    suspend fun getApiKey(): List<ApiKeyEntity>

    @Query("DELETE FROM apikeytable")
    suspend fun deleteAllApiKeys()
}

