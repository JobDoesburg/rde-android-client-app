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
import nl.surf.filesender.rde.client.RDEDocumentMRZData
import nl.surf.filesender.rde.client.activities.MainActivity
import nl.surf.filesender.rde.client.activities.general.ReadMRZActivity
import nl.surf.filesender.rde.client.activities.general.ScanQRActivity
import nl.surf.filesender.rde.data.RDEEnrollmentParameters

class EnrollmentActivity : AppCompatActivity() {
    private val client = HttpClient(OkHttp) { // We need to use OkHttp because of DNS + IPv6 issues with CIO engine
        install(ContentNegotiation) {
            json()
        }
    }

    private lateinit var socketUrl : String
    private lateinit var mrzData: RDEDocumentMRZData
    private lateinit var enrollmentParams: RDEEnrollmentParameters
    private lateinit var documentName: String
    private var withSecurityData = false
    private var withMRZData = false
    private var withFaceImageData = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchQRScanner()
    }

    fun launchQRScanner() {
        val intent = Intent(this, ScanQRActivity::class.java)
        startActivityForResult(intent, LAUNCH_QR_SCANNER)
    }

    private fun launchMRZInput() {
        val intent = Intent(this, ReadMRZActivity::class.java)
        startActivityForResult(intent, LAUNCH_MRZ_INPUT)
    }

    private fun launchEnrollmentOptionsInput() {
        val intent = Intent(this, EnrollmentOptionsActivity::class.java)
        startActivityForResult(intent, LAUNCH_OPTIONS)
    }

    private fun launchReadNFC() {
        val intent = Intent(this, EnrollmentReadNFCActivity::class.java)
        intent.putExtra("mrzData", mrzData)
        intent.putExtra("documentName", documentName)
        intent.putExtra("withSecurityData", withSecurityData)
        intent.putExtra("withMRZData", withMRZData)
        intent.putExtra("withFaceImageData", withFaceImageData)
        startActivityForResult(intent, LAUNCH_READ_NFC)
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
                launchEnrollmentOptionsInput()
            } else if (resultCode == RESULT_CANCELED) {
                launchQRScanner()
            }
        }
        if (requestCode == LAUNCH_OPTIONS) {
            if (resultCode == RESULT_OK) {
                documentName = data?.getStringExtra("documentName")!!
                withSecurityData = data.getBooleanExtra("withSecurityData", false)
                withMRZData = data.getBooleanExtra("withMRZData", false)
                withFaceImageData = data.getBooleanExtra("withFaceImageData", false)
                launchReadNFC()
            } else if (resultCode == RESULT_CANCELED) {
                launchMRZInput()
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
        const val LAUNCH_OPTIONS = 3
        const val LAUNCH_READ_NFC = 4
    }

}