package nl.surf.rde.app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import nl.surf.rde.app.decryption.DecryptionActivity
import nl.surf.rde.app.enrollment.EnrollmentActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val enrollmentButtonClick = findViewById<Button>(R.id.nextButton)
        enrollmentButtonClick.setOnClickListener {
            val intent = Intent(this, EnrollmentActivity::class.java)
            startActivity(intent)
        }

        val decryptButtonClick = findViewById<Button>(R.id.decryptButton)
        decryptButtonClick.setOnClickListener {
            val intent = Intent(this, DecryptionActivity::class.java)
            startActivity(intent)
        }
    }
}