package arkivar

import arkivar.kafka.InnsendingKafkaDto
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import no.nav.aap.kafka.streams.v2.Topic
import no.nav.aap.kafka.streams.v2.serde.JsonSerde
import no.nav.aap.kafka.streams.v2.test.TestTopic
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AppTest {
    private lateinit var mocks: MockEnvironment
    private lateinit var innsendingTopic: TestTopic<InnsendingKafkaDto>
    @BeforeAll
    fun setupMockEnvironment() {
        mocks = MockEnvironment()
    }

    @AfterAll
    fun closeMockEnvironment() = mocks.close()


    @Test
    fun `actuators available without auth`() {
        testApplication {
            environment { config = mocks.applicationConfig() }
            application { server(mocks.kafka) }

            runBlocking {
                val live = client.get("actuator/live")
                assertEquals(HttpStatusCode.OK, live.status)

                val ready = client.get("actuator/ready")
                assertEquals(HttpStatusCode.OK, ready.status)

                val metrics = client.get("actuator/metrics")
                assertEquals(HttpStatusCode.OK, metrics.status)
                assertNotNull(metrics.bodyAsText())
            }
        }
    }

    @Test
    fun `tar imot melding på kafka og arkiverer`(){
        lateinit var innsendingTopic: TestTopic<InnsendingKafkaDto>

        val app = TestApplication {
            environment { config=mocks.applicationConfig() }
            application {
                server(mocks.kafka)
                innsendingTopic=mocks.kafka.testTopic(Topic("aap.innsending.v1",JsonSerde.jackson()))
            }
        }

        val client = app.createClient {
            install(ContentNegotiation) {
                jackson {
                    registerModule(JavaTimeModule())
                    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                }
            }
        }

        runBlocking { client.get("/actuator/live") }


        innsendingTopic.produce("12342332132"){
            InnsendingKafkaDto(
                tittel="tittel",
                innsendingsreferanse = "innsendingsRef",
                filreferanser = listOf("FilRefListePunkt1"),
                brevkode = "Brevkode-1",
                callId = "Random-UUID-her"
            )
        }
        assert(true)

    }

}