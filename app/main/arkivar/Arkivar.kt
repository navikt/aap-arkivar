package arkivar

import arkivar.arkiv.Fødselsnummer
import arkivar.arkiv.JoarkClient
import arkivar.arkiv.Journalpost
import arkivar.kafka.InnsendingKafkaDto

class Arkivar(private val fillagerOppslag: FillagerOppslag, private val joarkClient: JoarkClient) {
    fun arkiverDokument(key: String, kafkaDto: InnsendingKafkaDto): String {
        val respons = fillagerOppslag.hentFiler(kafkaDto.innsendingsreferanse)

        val dokumenter = respons.filter { kafkaDto.filreferanser.contains(it.filreferanse.toString()) }.map { fil ->
            Journalpost.Dokument(
                tittel = fil.tittel,
                brevkode = kafkaDto.brevkode,
                dokumentVarianter = listOf(
                    Journalpost.DokumentVariant(
                        fysiskDokument = fil.fil,
                    )
                )
            )
        }

        val journalpost = Journalpost(
            tittel = kafkaDto.tittel,
            avsenderMottaker = Journalpost.AvsenderMottaker(
                id = Fødselsnummer(
                    fnr = key
                ),
            ),
            bruker = Journalpost.Bruker(
                id = Fødselsnummer(
                    fnr = key
                )
            ),
            dokumenter = dokumenter,
            eksternReferanseId = kafkaDto.innsendingsreferanse,
        )
        val joarkRespons = joarkClient.opprettJournalpost(journalpost, kafkaDto.callId)
        return joarkRespons.journalpostId
    }
}
