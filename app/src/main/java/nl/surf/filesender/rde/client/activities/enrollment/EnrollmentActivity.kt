package nl.surf.filesender.rde.client.activities.enrollment

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import nl.surf.filesender.rde.client.activities.MainActivity
import nl.surf.filesender.rde.client.activities.decryption.DecryptionActivity
import nl.surf.filesender.rde.client.activities.general.ScanQRActivity
import nl.surf.filesender.rde.client.RDEDocumentMRZData
import nl.surf.filesender.rde.data.RDEEnrollmentParameters

class EnrollmentActivity : AppCompatActivity() {
    private val client = HttpClient(OkHttp) { // We need to use OkHttp because of DNS + IPv6 issues with CIO engine
        install(ContentNegotiation) {
            json()
        }
    }

    private lateinit var socketUrl : String
    private lateinit var mrzData: RDEDocumentMRZData
    private lateinit var documentName: String
    private lateinit var enrollmentParams: RDEEnrollmentParameters

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchQRScanner()
    }

    fun launchQRScanner() {
        val intent = Intent(this, ScanQRActivity::class.java)
        startActivityForResult(intent, DecryptionActivity.LAUNCH_QR_SCANNER)
    }

    private fun launchMRZInput() {
        val intent = Intent(this, EnrollmentReadMRZActivity::class.java)
        startActivityForResult(intent, DecryptionActivity.LAUNCH_MRZ_INPUT)
    }

    private fun launchReadNFC() {
        val intent = Intent(this, EnrollmentReadNFCActivity::class.java)
        intent.putExtra("mrzData", mrzData)
        intent.putExtra("documentName", documentName)
        startActivityForResult(intent, DecryptionActivity.LAUNCH_READ_NFC)
    }

    private fun performEnrollment() {
        runBlocking {
            client.post(socketUrl) {
                contentType(ContentType.Application.Json)
                setBody(enrollmentParams)
            }
        }
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LAUNCH_QR_SCANNER) {
            if (resultCode == RESULT_OK) {
                socketUrl = data?.getStringExtra("result")!!
                launchMRZInput()
            } else if (resultCode == RESULT_CANCELED) {
                finish()
            }
        }
        if (requestCode == LAUNCH_MRZ_INPUT) {
            if (resultCode == RESULT_OK) {
                mrzData = data?.extras!!["result"] as RDEDocumentMRZData
                documentName = data.getStringExtra("documentName")!!
                launchReadNFC()
            } else if (resultCode == RESULT_CANCELED) {
                launchQRScanner()
            }
        }
        if (requestCode == LAUNCH_READ_NFC) {
            if (resultCode == RESULT_OK) {
                enrollmentParams = data?.extras!!["result"] as RDEEnrollmentParameters
                performEnrollment()
            } else if (resultCode == RESULT_CANCELED) {
                launchMRZInput()
            }
        }
    }

    override fun finish() {
        client.close()
        super.finish()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    companion object {
        const val LAUNCH_QR_SCANNER = 1
        const val LAUNCH_MRZ_INPUT = 2
        const val LAUNCH_READ_NFC = 3
    }

}