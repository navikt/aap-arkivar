package arkivar.kafka

import no.nav.aap.kafka.streams.v2.Topic
import no.nav.aap.kafka.streams.v2.serde.JsonSerde

data class InnsendingKafkaDto(
    val tittel: String,
    val innsendingsreferanse: String,
    val filreferanser: List<String>,
    val brevkode: String,
    val callId:String
)

object Topics {
    val innsending = Topic("aap.innsending.v1", JsonSerde.jackson<InnsendingKafkaDto>())
    val feiletInnsending = Topic("aap.innsending.dlq.v1", JsonSerde.jackson<InnsendingKafkaDto>())
}
