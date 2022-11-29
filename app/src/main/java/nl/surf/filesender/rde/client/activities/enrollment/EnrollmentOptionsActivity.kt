package nl.surf.filesender.rde.client.activities.enrollment

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import nl.surf.filesender.rde.client.R

class EnrollmentOptionsActivity : AppCompatActivity() {
    private lateinit var documentNameField: EditText
    private lateinit var withSecurityDataCheckBox: CheckBox
    private lateinit var withMRZDataCheckBox: CheckBox
    private lateinit var withFaceImageDataCheckBox: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enrollment_options)

        documentNameField = findViewById(R.id.documentName)
        withSecurityDataCheckBox = findViewById(R.id.withSecurityDataCheckBox)
        withMRZDataCheckBox = findViewById(R.id.withMRZDataCheckBox)
        withFaceImageDataCheckBox = findViewById(R.id.withFaceImageDataCheckBox)

        val nextButton = findViewById<Button>(R.id.nextButton)
        nextButton.setOnClickListener {
            onNextButtonClick()
        }
    }

    private fun onNextButtonClick() {
        val resultIntent = Intent()
        resultIntent.putExtra("documentName", documentNameField.text.toString());
        resultIntent.putExtra("withSecurityData", withSecurityDataCheckBox.isChecked);
        resultIntent.putExtra("withMRZData", withMRZDataCheckBox.isChecked);
        resultIntent.putExtra("withFaceImageData", withFaceImageDataCheckBox.isChecked);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }
}