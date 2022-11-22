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
import nl.surf.filesender.rde.data.RDEDocumentMRZData
import java.util.*

open class ReadMRZActivity : AppCompatActivity() {
    private lateinit var documentIdField: EditText
    private lateinit var dateOfBirthField: EditText
    private lateinit var dateOfExpiryField: EditText
    private lateinit var documentNameField: EditText
    // TODO: store the most recent MRZ Data for better usability

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read_mrz)

        documentIdField = findViewById(R.id.documentId)
        dateOfBirthField = findViewById(R.id.dateOfBirth)
        dateOfExpiryField = findViewById(R.id.dateOfExpiry)
        documentNameField = findViewById(R.id.documentName)

        addDatePickerDialog(dateOfBirthField)
        addDatePickerDialog(dateOfExpiryField)

        val enrollmentButtonClick = findViewById<Button>(R.id.nextButton)
        enrollmentButtonClick.setOnClickListener {
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

    private fun validateMRZData(): RDEDocumentMRZData? {
        val documentId = documentIdField.text.toString()
        val dateOfBirth = dateOfBirthField.text.toString()
        val dateOfExpiry = dateOfExpiryField.text.toString()
        val documentName = documentNameField.text.toString()

        if (documentId.length != 9 || dateOfBirth.length != 6 || dateOfExpiry.length != 6 || documentName.isEmpty()) {
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
