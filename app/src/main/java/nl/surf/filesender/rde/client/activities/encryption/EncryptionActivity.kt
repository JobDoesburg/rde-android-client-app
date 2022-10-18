package nl.surf.filesender.rde.client.activities.encryption

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.sf.scuba.util.Hex
import nl.surf.filesender.rde.client.R
import nl.surf.filesender.rde.client.RDEEnrollmentParameters
import nl.surf.filesender.rde.client.RDEKey
import nl.surf.filesender.rde.client.RDEKeyGenerator
import nl.surf.filesender.rde.client.activities.MainActivity

class EncryptionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_encryption_result)

        val key: RDEKey?
        val enrollmentParamsData = intent.getStringExtra("data")!!
        try {
            val enrollmentParams = Json.decodeFromString<RDEEnrollmentParameters>(enrollmentParamsData)
            val rdeKeyGenerator = RDEKeyGenerator(enrollmentParams)
            key = rdeKeyGenerator.generateKey()
        } catch (e: Exception) {
            Toast.makeText(this, "Invalid enrollment parameters", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val decryptionParamsJson = Json.encodeToString(key.decryptionParameters)
        val encodedKey = Hex.toHexString(key.encryptionKey)

        val encryptionKeyTextView = findViewById<TextView>(R.id.encryptionKeyField)
        val decryptionParametersTextView = findViewById<TextView>(R.id.decryptionParametersField)
        encryptionKeyTextView.text = encodedKey
        decryptionParametersTextView.text = decryptionParamsJson

        val doneButton = findViewById<Button>(R.id.doneButton)
        doneButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

    }
}