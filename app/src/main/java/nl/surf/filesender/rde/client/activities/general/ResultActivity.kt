package nl.surf.filesender.rde.client.activities.general

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import nl.surf.filesender.rde.client.R
import nl.surf.filesender.rde.client.activities.MainActivity

open class ResultActivity : AppCompatActivity() {
    lateinit var resultData: String
    lateinit var resultDataView: TextView
    lateinit var doneButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)
        resultDataView = findViewById(R.id.resultDataField)
        doneButton = findViewById(R.id.doneButton)

        resultData = intent.getStringExtra("result_data")!!
        resultDataView.text = resultData

        doneButton.setOnClickListener {
            onDoneButtonClick()
        }
    }

    open fun onDoneButtonClick() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

}