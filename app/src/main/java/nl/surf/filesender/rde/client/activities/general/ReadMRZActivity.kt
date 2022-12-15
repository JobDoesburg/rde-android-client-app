package nl.surf.filesender.rde.client.activities.general

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import nl.surf.filesender.rde.client.R
import nl.surf.filesender.rde.client.RDEDocumentMRZData
import java.util.*

open class ReadMRZActivity : AppCompatActivity() {
    private lateinit var documentIdField: EditText
    private lateinit var dateOfBirthField: EditText
    private lateinit var dateOfExpiryField: EditText
    // TODO: store the most recent MRZ Data for better usability
    // TODO: add CAN support

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read_mrz)

        documentIdField = findViewById(R.id.documentId)
        dateOfBirthField = findViewById(R.id.dateOfBirth)
        dateOfExpiryField = findViewById(R.id.dateOfExpiry)

        addDatePickerDialog(dateOfBirthField)
        addDatePickerDialog(dateOfExpiryField)

        val nextButton = findViewById<Button>(R.id.nextButton)
        nextButton.setOnClickListener {
            onNextButtonClick()
        }
    }

    private fun addDatePickerDialog(field: EditText) {
        field.setOnClickListener {
            val c = Calendar.getInstance()
            val y = c.get(Calendar.YEAR)
            val m = c.get(Calendar.MONTH)
            val d = c.get(Calendar.DAY_OF_MONTH)
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, day ->
                    val value = year.toString().takeLast(2) + (month + 1).toString()
                        .padStart(2, '0') + day.toString().padStart(2, '0');
                    field.setText(value)
                },
                y,
                m,
                d
            )
            datePickerDialog.show()
        }
    }

    open fun validateMRZData(): RDEDocumentMRZData? {
        val documentId = documentIdField.text.toString()
        val dateOfBirth = dateOfBirthField.text.toString()
        val dateOfExpiry = dateOfExpiryField.text.toString()

        if (documentId.length != 9) {
            documentIdField.error = "Document ID must be 9 characters long"
            return null
        }
        if (dateOfBirth.length != 6) {
            dateOfBirthField.error = "Date of birth must be 6 characters long"
            return null
        }
        if (dateOfExpiry.length != 6) {
            dateOfExpiryField.error = "Date of expiry must be 6 characters long"
            return null
        }

        return RDEDocumentMRZData(documentId = documentId, dateOfBirth = dateOfBirth, dateOfExpiry = dateOfExpiry)
    }


    private fun onNextButtonClick() {
        val mrzData = validateMRZData()
        if (mrzData == null) {
            Toast.makeText(this, "Data not valid", Toast.LENGTH_SHORT).show()
            return
        }

        val resultIntent = Intent()
        resultIntent.putExtra("result", mrzData);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

}
