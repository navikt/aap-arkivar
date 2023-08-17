package arkivar.mock

import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.intellij.lang.annotations.Language

internal fun Application.azureAdMock() {
    install(ContentNegotiation) { jackson {} }
    routing {
        post("/token") {
            require(call.receiveText() == "client_id=oppgavestyring&client_secret=test&scope=test&grant_type=client_credentials")
            call.respond(HttpStatusCode.OK, Token())
        }
    }
}

private data class Token(
    val token_type: String = "Bearer",
    val expires_in: Long = 3599,
    val access_token: String = "very.secure.token"
)

@Language("JSON")
private const val validToken = """
{
  "token_type": "Bearer",
  "expires_in": 3599,
  "access_token": "very.secure.token"
}
"""