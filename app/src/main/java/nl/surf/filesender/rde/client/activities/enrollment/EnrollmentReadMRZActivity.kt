package nl.surf.filesender.rde.client.activities.enrollment

import android.content.Intent
import nl.surf.filesender.rde.client.activities.general.ReadMRZActivity
import nl.surf.filesender.rde.client.RDEDocumentMRZData


class EnrollmentReadMRZActivity : ReadMRZActivity() {
    override fun validateMRZData(): RDEDocumentMRZData? {
        val documentName = documentNameField.text.toString()
        if (documentName.isEmpty()) {
            documentNameField.error = "Document name is required"
            return null
        }

        return super.validateMRZData()
    }

    override fun addIntentExtras(intent: Intent) {
        intent.putExtra("documentName", documentNameField.text.toString())
    }

}