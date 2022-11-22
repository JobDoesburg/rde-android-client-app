package nl.surf.filesender.rde.client.activities.general

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import nl.surf.filesender.rde.client.R

open class ParamsInputActivity : AppCompatActivity() {
    lateinit var inputDataField: EditText

    open lateinit var nextActivity: Class<*>

    var receivedIntentExtras: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_params_input)

        receivedIntentExtras = intent.extras

        inputDataField = findViewById(R.id.dataInputTextField)

        val nextButton = findViewById<Button>(R.id.nextButton)
        nextButton.setOnClickListener {
            val intent = getNextActivityIntent()
            startActivity(intent)
        }
    }

    open fun getNextActivityIntent() : Intent {
        val intent = Intent(this, nextActivity)
        if (receivedIntentExtras != null) {
            intent.putExtras(receivedIntentExtras!!)
        }
        intent.putExtra("data", inputDataField.text.toString())
        return intent
    }
}