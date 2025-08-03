package io.availe.demo.playground

import io.availe.Replicate
import io.availe.models.AutoContextual
import io.availe.models.DtoVariant
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant

object InstantAsStringSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Instant {
        return Instant.parse(decoder.decodeString())
    }
}

private interface UserAccount

@Replicate.Model(variants = [DtoVariant.DATA], autoContextual = AutoContextual.DISABLED)
@Replicate.Apply([Serializable::class])
private interface V1 : UserAccount {
    val id: Int

    @Contextual
    val startTime: Instant

    @Serializable(with = InstantAsStringSerializer::class)
    val midTime: Instant

    @Replicate.Property(autoContextual = AutoContextual.ENABLED)
    val endTime: List<List<Instant>>
}