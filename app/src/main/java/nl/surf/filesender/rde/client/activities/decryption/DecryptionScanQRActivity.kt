package nl.surf.filesender.rde.client.activities.decryption

import android.content.Intent

import nl.surf.filesender.rde.client.activities.general.ScanQRActivity

class DecryptionScanQRActivity : ScanQRActivity() {

    override fun processQRCodeScan(data: String) {
        super.processQRCodeScan(data)
        // todo validate qr code content, check if it is a valid enrollment url and if server is online
        val intent = Intent(this, DecryptionParamsInputActivity::class.java)
        intent.putExtra("decryption_socket_url", data)
        startActivity(intent)
    }
}