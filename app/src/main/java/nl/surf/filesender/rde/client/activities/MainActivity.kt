package nl.surf.filesender.rde.client.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import nl.surf.filesender.rde.client.R
import nl.surf.filesender.rde.client.activities.decryption.DecryptionParamsInputActivity
import nl.surf.filesender.rde.client.activities.decryption.ExtractDecryptionKeyReadNFCActivity
import nl.surf.filesender.rde.client.activities.encryption.EncryptionParamsInputActivity
import nl.surf.filesender.rde.client.activities.enrollment.EnrollmentReadMRZActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val enrollmentButtonClick = findViewById<Button>(R.id.nextButton)
        enrollmentButtonClick.setOnClickListener {
            val intent = Intent(this, EnrollmentReadMRZActivity::class.java)
            startActivity(intent)
        }

        val decryptButtonClick = findViewById<Button>(R.id.decryptButton)
        decryptButtonClick.setOnClickListener {
            val intent = Intent(this, DecryptionParamsInputActivity::class.java)
            startActivity(intent)
        }


        val encryptButtonClick = findViewById<Button>(R.id.encryptButton)
        encryptButtonClick.setOnClickListener {
            val intent = Intent(this, EncryptionParamsInputActivity::class.java)
            startActivity(intent)

        }


    }
}