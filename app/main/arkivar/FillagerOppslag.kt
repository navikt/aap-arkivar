package arkivar

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
import java.time.LocalDateTime
import java.util.*


private const val FILLAGER_CLIENT_SECONDS_METRICNAME = "fillager_client_seconds"
private val secureLog = LoggerFactory.getLogger("secureLog")
private val clientLatencyStats: Summary = Summary.build()
    .name(FILLAGER_CLIENT_SECONDS_METRICNAME)
    .quantile(0.5, 0.05) // Add 50th percentile (= median) with 5% tolerated error
    .quantile(0.9, 0.01) // Add 90th percentile with 1% tolerated error
    .quantile(0.99, 0.001) // Add 99th percentile with 0.1% tolerated error
    .help("Latency fillager, in seconds")
    .register()

class FillagerOppslag(azureConfig: AzureConfig) {

    private val tokenProvider = AzureAdTokenProvider(azureConfig, "fillagerScope")


    fun hentFiler(
        innendingsreferanse: String
    ): List<FilDTO> = clientLatencyStats.startTimer().use {
        runBlocking {
                val token = tokenProvider.getClientCredentialToken()
                httpClient.get("/innsending/$innendingsreferanse") {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                }.body<List<FilDTO>>()
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

data class FilDTO(val filreferanse: UUID, val tittel: String, val fil: String, val opprettet: LocalDateTime)
