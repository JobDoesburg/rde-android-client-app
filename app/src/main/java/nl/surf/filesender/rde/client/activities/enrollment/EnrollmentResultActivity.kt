package nl.surf.filesender.rde.client.activities.enrollment

import android.os.Bundle
import android.util.Log
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import nl.surf.filesender.rde.client.activities.general.ResultActivity

class EnrollmentResultActivity : ResultActivity() {
    private lateinit var enrollmentCallbackUrl: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enrollmentCallbackUrl = intent.getStringExtra("enrollment_callback_url")!!
    }

    private fun pushEnrollmentDataToServer(url : String, data : String) {
        Log.d("EnrollmentResult", "Pushing enrollment data to server")
        Log.d("EnrollmentResult", "URL: $url")
        Log.d("EnrollmentResult", "Data: $data")

        runBlocking {
            val client = HttpClient(CIO)
            client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(data)
            }
            client.close()
        }
    }

    override fun onDoneButtonClick() {
        val url = enrollmentCallbackUrl
        val data = resultData
        pushEnrollmentDataToServer(url, data)

        super.onDoneButtonClick()
    }

}