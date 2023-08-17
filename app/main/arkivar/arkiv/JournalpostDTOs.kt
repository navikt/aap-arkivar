package arkivar.arkiv

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonValue

private const val INNGÅENDE = "INNGAAENDE"
private const val KANAL = "NAV_NO"
private const val ID_TYPE = "FNR"

data class Journalpost(
    val tittel: String,
    val avsenderMottaker: AvsenderMottaker,
    val bruker: Bruker,
    val dokumenter: List<Dokument>,
    val eksternReferanseId: String,
    val kanal: String = KANAL,
    val journalposttype: String = INNGÅENDE,
    val tilleggsopplysninger: List<Tilleggsopplysning> = listOf(Tilleggsopplysning("versjon", "1.0")),
    val tema: String = "AAP"
) {

    data class Tilleggsopplysning(val nokkel: String, val verdi: String)

    data class Dokument(
        val tittel: String?,
        val brevkode: String? = null,
        val dokumentVarianter: List<DokumentVariant>
    )

    data class DokumentVariant (
        val filtype: String = "PDFA",
        val fysiskDokument: String,
        val variantformat: String= "ARKIV"
    )

    data class Bruker(val id: Fødselsnummer, val idType: String = ID_TYPE)
    data class AvsenderMottaker(
        val id: Fødselsnummer,
        val navn: String?=null,
        val idType: String = ID_TYPE
    )
}

data class Fødselsnummer(@get:JsonValue val fnr: String) {
    override fun toString() = "${javaClass.simpleName} [fnr=${fnr.partialMask()}]"
}

fun String.partialMask(mask: Char = '*'): String {
    val start = length.div(2)
    return replaceRange(start + 1, length, mask.toString().repeat(length - start - 1))
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class ArkivResponse(val journalpostId: String,
                         val journalpostferdigstilt: Boolean,
                         val dokumenter: List<DokumentId>) {
    data class DokumentId(val dokumentInfoId: String)
    }