package arkivar

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AppTest {
    private lateinit var mocks: MockEnvironment

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

}