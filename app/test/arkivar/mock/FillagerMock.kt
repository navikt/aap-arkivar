package arkivar.mock

import arkivar.Fil
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

internal fun Application.fillagerMock() {
    install(ContentNegotiation) { jackson {} }
    routing {
        get("/{filreferanse}") {
            call.respond(HttpStatusCode.OK, Fil("",""))
        }
    }
}