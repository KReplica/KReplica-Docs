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

// For the purpose of this example, we're creating a custom KSerializer.
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

// KReplica has automatic @Contextual generation by default, but for
// this example, we disabled it via "autoContextual = AutoContextual.DISABLED"
@Replicate.Model(variants = [DtoVariant.DATA], autoContextual = AutoContextual.DISABLED)
@Replicate.Apply([Serializable::class])
private interface V1 : UserAccount {
    val id: Int

    // Since auto-contextual is off, we must manually apply @Contextual for this complex type.
    @Contextual
    val startTime: Instant

    // Alternatively, you can explicitly provide a custom serializer.
    @Serializable(with = InstantAsStringSerializer::class)
    val midTime: Instant

    // Or, you can override the model-level setting and re-enable automatic @Contextual
    // generation for just this property. KReplica will handle it from here.
    @Replicate.Property(autoContextual = AutoContextual.ENABLED)
    val endTime: List<List<Instant>>
}