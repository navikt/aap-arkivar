package arkivar.mock

import arkivar.AppTest
import arkivar.Fil
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import java.io.File
import java.lang.Exception
import java.util.Base64

internal fun Application.fillagerMock() {
    install(ContentNegotiation) { jackson {} }
    routing {
        get("/{filreferanse}") {
            val filref = call.parameters.getOrFail("filreferanse")
            try {
                val fil = hentFil(filref)
                call.respond(HttpStatusCode.OK, Fil(filref, fil))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest)
            }
        }
    }
}

private fun hentFil(filref: String): String {
    val resource = requireNotNull(AppTest::class.java.getResource("/resources/$filref"))
    val bytes = resource.readBytes()
    val enc = Base64.getEncoder().encode(bytes)
    return String(enc)
}