package nl.surf.filesender.rde.client.activities.enrollment

import android.content.Intent

import nl.surf.filesender.rde.client.activities.general.ScanQRActivity

class EnrollmentScanQRActivity : ScanQRActivity() {

    override fun processQRCodeScan(data: String) {
        super.processQRCodeScan(data)
        // todo validate qr code content, check if it is a valid enrollment url and if server is online
        val intent = Intent(this, EnrollmentReadMRZActivity::class.java)
        intent.putExtra("enrollment_callback_url", data)
        startActivity(intent)
    }
}