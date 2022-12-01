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

        withMRZDataCheckBox.setOnClickListener { onMRZCheckBoxClick() }
        withFaceImageDataCheckBox.setOnClickListener { onFaceImageCheckBoxClick() }

        val nextButton = findViewById<Button>(R.id.nextButton)
        nextButton.setOnClickListener {
            onNextButtonClick()
        }
    }

    private fun onMRZCheckBoxClick() {
        if (withMRZDataCheckBox.isChecked) {
            withSecurityDataCheckBox.isChecked = true
            withSecurityDataCheckBox.isEnabled = false
        } else {
            withSecurityDataCheckBox.isEnabled = true
        }
    }

    private fun onFaceImageCheckBoxClick() {
        if (withFaceImageDataCheckBox.isChecked) {
            withSecurityDataCheckBox.isChecked = true
            withSecurityDataCheckBox.isEnabled = false
        } else {
            withSecurityDataCheckBox.isEnabled = true
        }
    }

    private fun onNextButtonClick() {
        val documentName = documentNameField.text.toString()
        val withSecurityData = withSecurityDataCheckBox.isChecked
        val withMRZData = withMRZDataCheckBox.isChecked
        val withFaceImageData = withFaceImageDataCheckBox.isChecked

        if (documentName.isEmpty() || documentName.isBlank()) {
            Toast.makeText(this, "Document name is required", Toast.LENGTH_SHORT).show()
            return
        }

        val resultIntent = Intent()
        resultIntent.putExtra("documentName", documentName);
        resultIntent.putExtra("withSecurityData", withSecurityData);
        resultIntent.putExtra("withMRZData", withMRZData);
        resultIntent.putExtra("withFaceImageData", withFaceImageData);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }
}