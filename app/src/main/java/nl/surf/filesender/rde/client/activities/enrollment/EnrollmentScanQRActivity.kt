package nl.surf.filesender.rde.client.activities.enrollment

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import nl.surf.filesender.rde.client.R

import com.budiyev.android.codescanner.*
import com.google.zxing.BarcodeFormat

class EnrollmentScanQRActivity : AppCompatActivity() {
    private lateinit var codeScanner: CodeScanner
    private lateinit var codeScannerView: CodeScannerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enrollment_scan_qractivity)
        codeScannerView = findViewById(R.id.scanner_view)

        startQRScanner()
    }

    override fun onResume() {
        super.onResume()
        codeScanner.startPreview()
    }

    override fun onPause() {
        codeScanner.releaseResources()
        super.onPause()
    }

    private fun startQRScanner() {
        codeScanner = CodeScanner(this, codeScannerView)
        codeScanner.camera = CodeScanner.CAMERA_BACK
        codeScanner.formats = listOf(BarcodeFormat.QR_CODE)
        codeScanner.autoFocusMode = AutoFocusMode.SAFE
        codeScanner.scanMode = ScanMode.SINGLE
        codeScanner.isAutoFocusEnabled = true
        codeScanner.isFlashEnabled = false
        codeScanner.decodeCallback = DecodeCallback {
            runOnUiThread {
                processQRCodeScan(it.text)
            }
        }
        codeScanner.errorCallback = ErrorCallback {
            runOnUiThread {
                Toast.makeText(
                    this, "Camera initialization error: ${it.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        codeScannerView.setOnClickListener {
            codeScanner.startPreview()
        }
    }

    private fun processQRCodeScan(qrCode: String) {
        // todo validate qr code content, check if it is a valid enrollment url and if server is online
        val intent = Intent(this, EnrollmentReadMRZActivity::class.java)
        intent.putExtra("enrollment_callback_url", qrCode)
        startActivity(intent)
    }
}