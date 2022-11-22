package nl.surf.filesender.rde.client.handshake

import io.ktor.websocket.*
import io.mockk.mockk
import org.junit.Before
import org.junit.Test

internal class DecryptionHandshakeProtocolTest {
    private lateinit var protocol : DecryptionHandshakeProtocol

    @Before
    fun setUp() {
        val session = mockk<DefaultWebSocketSession>()
        protocol = DecryptionHandshakeProtocol(session)
        protocol.generateKeyPair()
    }

    @Test
    fun testPerformHandshake() {
    }
}