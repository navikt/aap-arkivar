package arkivar.arkiv

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.prometheus.client.Summary
import kotlinx.coroutines.runBlocking
import no.nav.aap.ktor.client.AzureAdTokenProvider
import no.nav.aap.ktor.client.AzureConfig
import org.slf4j.LoggerFactory

private const val JOARK_CLIENT_SECONDS_METRICNAME = "joark_client_seconds"
private val secureLog = LoggerFactory.getLogger("secureLog")
private val clientLatencyStats: Summary = Summary.build()
    .name(JOARK_CLIENT_SECONDS_METRICNAME)
    .quantile(0.5, 0.05) // Add 50th percentile (= median) with 5% tolerated error
    .quantile(0.9, 0.01) // Add 90th percentile with 1% tolerated error
    .quantile(0.99, 0.001) // Add 99th percentile with 0.1% tolerated error
    .help("Latency joark, in seconds")
    .register()

class JoarkClient(azureConfig: AzureConfig) {

    private val tokenProvider = AzureAdTokenProvider(azureConfig, "fillagerScope")

    fun opprettJournalpost(
        journalpost: Journalpost,
        callId:String
    ): ArkivResponse =
        clientLatencyStats.startTimer().use {
            runBlocking {
                val token = tokenProvider.getClientCredentialToken()
                httpClient.post("url") {
                    accept(ContentType.Application.Json)
                    header("Nav-Callid", callId)
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody(journalpost)
                }.body()
            }
        }

    private val httpClient = HttpClient(CIO) {
        install(HttpTimeout)
        install(HttpRequestRetry)
        install(Logging) {
            level = LogLevel.BODY
        }

        install(ContentNegotiation) {
            jackson {
                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                registerModule(JavaTimeModule())
            }
        }
    }

}