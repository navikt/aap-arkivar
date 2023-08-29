package arkivar.mock

import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

internal fun Application.joarkMock() {
    install(ContentNegotiation) { jackson {} }
    routing {
        post("/rest/journalpostapi/v1/journalpost") {
            call.respond(HttpStatusCode.OK)
        }
    }
}
