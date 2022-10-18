package nl.surf.filesender.rde.client.activities.general

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import nl.surf.filesender.rde.client.R
import nl.surf.filesender.rde.client.activities.MainActivity

open class ResultActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val resultData = intent.getStringExtra("result_data")
        val resultDataView = findViewById<TextView>(R.id.resultDataField)
        resultDataView.text = resultData

        val doneButton = findViewById<Button>(R.id.doneButton)
        doneButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

}