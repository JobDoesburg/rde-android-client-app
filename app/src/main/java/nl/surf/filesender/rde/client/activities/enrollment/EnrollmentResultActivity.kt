package nl.surf.filesender.rde.client.activities.enrollment

import android.os.Bundle
import android.util.Log
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
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
            val client = HttpClient()
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