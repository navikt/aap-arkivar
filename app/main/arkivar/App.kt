package arkivar

import arkivar.arkiv.Fødselsnummer
import arkivar.arkiv.JoarkClient
import arkivar.arkiv.Journalpost
import arkivar.kafka.InnsendingKafkaDto
import arkivar.kafka.Topics
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.logging.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.aap.kafka.streams.v2.KafkaStreams
import no.nav.aap.kafka.streams.v2.Streams
import no.nav.aap.kafka.streams.v2.Topology
import no.nav.aap.kafka.streams.v2.config.StreamsConfig
import no.nav.aap.ktor.client.AzureConfig
import no.nav.aap.ktor.config.loadConfig
import org.slf4j.LoggerFactory
import java.lang.Exception

private val secureLog = LoggerFactory.getLogger("secureLog")

data class Config(
    val kafka: StreamsConfig,
    val azure: AzureConfig,
    val joark: JoarkConfig
)

data class JoarkConfig (
    val baseUrl: String
)

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::server).start(wait = true)
}

fun Application.server(kafka: Streams = KafkaStreams()) {
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val config = loadConfig<Config>()

    install(MicrometerMetrics) { registry = prometheus }

    Thread.currentThread().setUncaughtExceptionHandler { _, e -> secureLog.error("Uhåndtert feil", e) }
    environment.monitor.subscribe(ApplicationStopping) { kafka.close() }

    val fillagerOppslag=FillagerOppslag(config.azure)
    val joarkClient = JoarkClient(config.azure,config.joark)

    val arkivar=Arkivar(fillagerOppslag,joarkClient)

    kafka.connect(
        config = config.kafka,
        registry = prometheus,
        topology = topology(arkivar)
    )

    routing {
        route("/actuator") {
            get("/metrics") {
                call.respond(prometheus.scrape())
            }
            get("/live") {
                val status = if (kafka.live()) HttpStatusCode.OK else HttpStatusCode.InternalServerError
                call.respond(status, "arkivar")
            }
            get("/ready") {
                val status = if (kafka.ready()) HttpStatusCode.OK else HttpStatusCode.InternalServerError
                call.respond(status, "arkivar")
            }

        }
    }
}

internal fun topology(arkivar: Arkivar): Topology {
    return no.nav.aap.kafka.streams.v2.topology {
        consume(Topics.innsending)
            .map { key, value ->
                try {
                    arkivar.arkiverDokument(key,value)
                    ArkivOutcome(key, value,null)
                } catch (e: Exception) {
                    ArkivOutcome(key, value, e)
                }
            }
            .filter { value ->
                value.erIkkeVellykket()
            }
            .secureLog { value ->
                val innsending = value.value
                secureLog.error("Klarte ikke arkivere (innsendingref = ${innsending.innsendingsreferanse}, callid = ${innsending.callId})", value.exception)
            }
            .map { value ->
                value.value
            }
            .produce(Topics.feiletInnsending)
    }
}

data class ArkivOutcome(
    val key: String,
    val value: InnsendingKafkaDto,
    val exception: Exception?
) {
    fun erIkkeVellykket() = exception != null
}
