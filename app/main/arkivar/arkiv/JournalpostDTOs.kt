package arkivar.arkiv

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonValue
import java.util.Locale.getDefault


data class Journalpost(
        val tittel: String,
        val avsenderMottaker: AvsenderMottaker,
        val bruker: Bruker,
        val dokumenter: List<Dokument>,
        val eksternReferanseId: String,
        val kanal: String = KANAL,
        val journalposttype: String = INNGÅENDE,
        val tilleggsopplysninger: List<Tilleggsopplysning> = listOf(Tilleggsopplysning("versjon", "1.0")),
        val tema: String = "AAP") {

    data class Tilleggsopplysning(val nokkel: String, val verdi: String)

    data class Dokument private constructor(val tittel: String?, val brevkode: String? = null, val dokumentVarianter: List<DokumentVariant>) {
        constructor(dokumentVarianter: List<DokumentVariant>, type: SkjemaType = SkjemaType.STANDARD) : this(type.tittel, type.kode, dokumentVarianter)

        constructor(tittel: String? = null,  brevkode: String?,variant: DokumentVariant) : this(tittel, brevkode, listOf(variant))
    }

    data class DokumentVariant private constructor(val filtype: String, val fysiskDokument: String, val variantformat: String) {
        constructor(fysiskDokument: String, variantformat: VariantFormat = VariantFormat.ARKIV, filtype: Filtype = Filtype.PDFA) : this(filtype.name, fysiskDokument, variantformat.name)

        override fun toString() = "${javaClass.simpleName} [filtype=$filtype,format=$variantformat,fysiskDokument=${fysiskDokument.length} bytes]"

        enum class VariantFormat { ORIGINAL, ARKIV, FULLVERSJON }

        enum class Filtype { PDFA, JPEG, PNG, JSON }
    }

    data class Bruker(val id: Fødselsnummer, val idType: String = ID_TYPE)
    data class AvsenderMottaker private constructor(val id: Fødselsnummer, val navn: String?, val idType: String = ID_TYPE) {
        constructor (id: Fødselsnummer, navn: Navn) : this(id, navn.navn)
    }

    companion object {
        private const val INNGÅENDE = "INNGAAENDE"
        private const val KANAL = "NAV_NO"
        private const val ID_TYPE = "FNR"
    }

    override fun toString() = "${javaClass.simpleName} [tittel=$tittel,dokumenter=$dokumenter,eksternReferanseId=$eksternReferanseId, tilleggsopplysninger=$tilleggsopplysninger]"

}

data class Navn(val fornavn : String?, val mellomnavn : String?, val etternavn : String?) {

    @JsonIgnore
    val navn = listOfNotNull(fornavn, mellomnavn, etternavn).joinToString(separator = " ").trim()
}

enum class SkjemaType(val kode : String, val tittel : String) {
    MELDEKORT("NAV 00-10.02", "Meldekort"),
    KORRIGERT_MELDEKORT("NAV 00-10.03", "Korrigert meldekort"),
    UTLAND_SØKNAD("NAV 11-03.07", "Søknad om å beholde AAP ved opphold i utlandet"),
    UTLAND_ETTERSENDING("NAVe 11-03.07", "Ettersending til ${UTLAND_SØKNAD.tittel.decap()}"),
    STANDARD("NAV 11-13.05", "Søknad om arbeidsavklaringspenger"),
    STANDARD_ETTERSENDING("NAVe 11-13.05", "Ettersendelse til søknad om arbeidsavklaringspenger")
}

data class Fødselsnummer(@get:JsonValue val fnr : String) {
    init {
        require(fnr.length == 11) { "Fødselsnummer $fnr er ikke 11 siffer" }
        require(mod11(W1, fnr) == fnr[9] - '0') { "Første kontrollsiffer $fnr[9] ikke validert" }
        require(mod11(W2, fnr) == fnr[10] - '0') { "Andre kontrollsiffer $fnr[10] ikke validert" }
    }

    companion object {

        private val W1 = intArrayOf(2, 5, 4, 9, 8, 1, 6, 7, 3)
        private val W2 = intArrayOf(2, 3, 4, 5, 6, 7, 2, 3, 4, 5)

        private fun mod11(weights : IntArray, fnr : String) =
            with(weights.indices.sumOf { weights[it] * (fnr[(weights.size - 1 - it)] - '0') } % 11) {
                when (this) {
                    0 -> 0
                    1 -> throw IllegalArgumentException(fnr)
                    else -> 11 - this
                }
            }
    }

    override fun toString() = "${javaClass.simpleName} [fnr=${fnr.partialMask()}]"
}

fun String.partialMask(mask : Char = '*') : String {
    val start = length.div(2)
    return replaceRange(start + 1, length, mask.toString().repeat(length - start - 1))
}

fun String.decap() = replaceFirstChar { it.lowercase(getDefault()) }