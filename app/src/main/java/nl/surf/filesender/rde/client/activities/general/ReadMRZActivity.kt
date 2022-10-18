package nl.surf.filesender.rde.client.activities.general

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import nl.surf.filesender.rde.client.R
import java.util.*

open class ReadMRZActivity : AppCompatActivity() {
    private lateinit var documentIdField: EditText
    private lateinit var dateOfBirthField: EditText
    private lateinit var dateOfExpiryField: EditText
    private lateinit var documentNameField: EditText

    open lateinit var nextActivity: Class<*>

    private var receivedIntentExtras: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read_mrz)

        receivedIntentExtras = intent.extras

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

    private fun validateMRZData(): Boolean {
        val documentId = documentIdField.text.toString()
        val dateOfBirth = dateOfBirthField.text.toString()
        val dateOfExpiry = dateOfExpiryField.text.toString()
        val documentName = documentNameField.text.toString()

        if (documentId.length != 9 || dateOfBirth.length != 6 || dateOfExpiry.length != 6 || documentName.isEmpty()) {
            return false
        }

        return true
    }

    private fun onNextButtonClick() {
        if (validateMRZData()) {
            val intent = Intent(this, nextActivity)
            if (receivedIntentExtras != null) {
                intent.putExtras(receivedIntentExtras!!)
            }
            intent.putExtra("document_id",documentIdField.text.toString());
            intent.putExtra("date_of_birth",dateOfBirthField.text.toString());
            intent.putExtra("date_of_expiry",dateOfExpiryField.text.toString());
            intent.putExtra("document_name",documentNameField.text.toString());
            startActivity(intent)
        } else {
            Toast.makeText(
                applicationContext,
                "Date not valid, try again.", Toast.LENGTH_SHORT
            ).show()
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

}