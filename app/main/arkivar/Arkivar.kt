package arkivar

import arkivar.arkiv.Fødselsnummer
import arkivar.arkiv.JoarkClient
import arkivar.arkiv.Journalpost
import arkivar.kafka.InnsendingKafkaDto

class Arkivar (val fillagerOppslag: FillagerOppslag, val joarkClient: JoarkClient){
    fun arkiverDokument(key:String, kafkaDto: InnsendingKafkaDto){
        kafkaDto.filreferanser.forEach{ filReferanse ->
            val fil = fillagerOppslag.hentFil(kafkaDto.innsendingsreferanse, filReferanse)
            val journalpost = Journalpost(
                tittel = fil.tittel,
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
                dokumenter = listOf(
                    Journalpost.Dokument(
                        tittel = fil.tittel,
                        brevkode = kafkaDto.brevkode,
                        dokumentVarianter = listOf(
                            Journalpost.DokumentVariant(
                                fysiskDokument = fil.fysiskDokument,
                            )
                        )
                    )
                ),
                eksternReferanseId = kafkaDto.innsendingsreferanse,
            )
            joarkClient.opprettJournalpost(journalpost, kafkaDto.callId)
        }
    }
}