package nl.surf.filesender.rde.client.activities.general

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import nl.surf.filesender.rde.client.R

open class ParamsInputActivity : AppCompatActivity() {
    private lateinit var inputDataField: EditText

    open lateinit var nextActivity: Class<*>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_params_input)

        inputDataField = findViewById(R.id.dataInputTextField)

        val nextButton = findViewById<Button>(R.id.nextButton)
        nextButton.setOnClickListener {
            val intent = Intent(this, nextActivity)
            intent.putExtra("data", inputDataField.text.toString())
            startActivity(intent)
        }
    }
}