package arkivar

import arkivar.arkiv.Fødselsnummer
import arkivar.arkiv.JoarkClient
import arkivar.arkiv.Journalpost
import arkivar.kafka.InnsendingKafkaDto

class Arkivar (val fillagerOppslag: FillagerOppslag, val joarkClient: JoarkClient){
    fun arkiverDokument(key:String, kafkaDto: InnsendingKafkaDto){
        val respons = fillagerOppslag.hentFiler(kafkaDto.innsendingsreferanse, kafkaDto.filreferanser)

        val dokumenter = respons.filer.map { fil ->
            Journalpost.Dokument(
                tittel = fil.tittel,
                brevkode = kafkaDto.brevkode,
                dokumentVarianter = listOf(
                    Journalpost.DokumentVariant(
                        fysiskDokument = fil.fysiskDokument,
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
            joarkClient.opprettJournalpost(journalpost, kafkaDto.callId)
        }
    }
