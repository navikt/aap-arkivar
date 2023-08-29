package arkivar

import arkivar.mock.azureAdMock
import arkivar.mock.fillagerMock
import arkivar.mock.joarkMock
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import no.nav.aap.kafka.streams.v2.test.StreamsMock

class MockEnvironment : AutoCloseable {
    private val azureAd = embeddedServer(Netty, port = 0, module = Application::azureAdMock).apply { start() }
    private val joark = embeddedServer(Netty, port = 0, module = Application::joarkMock).apply { start() }
    private val fillager = embeddedServer(Netty, port = 80, module = Application::fillagerMock).apply { start() }

    val kafka = StreamsMock()

    override fun close() {
        azureAd.stop()
        joark.stop()
        fillager.stop()
    }

    fun applicationConfig() = MapApplicationConfig(
        "AZURE_OPENID_CONFIG_ISSUER" to "azure",
        "AZURE_APP_CLIENT_ID" to "oppgavestyring",
        "AZURE_OPENID_CONFIG_JWKS_URI" to "http://localhost:9999/jwks",
        "AZURE_OPENID_CONFIG_TOKEN_ENDPOINT" to "http://localhost:${azureAd.port}/token",
        "AZURE_APP_CLIENT_SECRET" to "test",
        "KAFKA_STREAMS_APPLICATION_ID" to "oppgavestyring",
        "KAFKA_BROKERS" to "mock://kafka",
        "KAFKA_TRUSTSTORE_PATH" to "",
        "KAFKA_KEYSTORE_PATH" to "",
        "KAFKA_CREDSTORE_PASSWORD" to "",
        "JOARK_BASE_URL" to "http://localhost:${joark.port}"
    )

    companion object {
        val NettyApplicationEngine.port get() = runBlocking { resolvedConnectors() }.first { it.type == ConnectorType.HTTP }.port
    }
}

//inline fun <reified V : Any> TestInputTopic<String, V>.produce(key: String, value: () -> V) = pipeInput(key, value())
//inline fun <reified V : Any> TestInputTopic<String, V>.produceTombstone(key: String) = pipeInput(key, null)