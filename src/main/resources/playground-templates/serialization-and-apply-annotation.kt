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

/*
@Replicate.Apply allows for two things:
1) Applying annotations that cannot be directly applied to
interfaces (such as kotlinx serialization)

2) Making it so tha an an annotation is only applied to specific
KReplica-variants instead of all variants.

By default, @Replicate.Apply affects all variants, unless you specify
otherwise with the include/exclude arguments.
 */

private interface UserAccount {
    // Example 1: KReplica has automatic @Contextual generation by default
    // As we didn't specify, @Replicate.Apply affects all variants
    @Replicate.Model(variants = [DtoVariant.DATA])
    @Replicate.Apply([Serializable::class])
    private interface V1 : UserAccount {
        val id: Int
        val startTime: Instant
        val midTime: Instant
        val endTime: List<List<Instant>>
    }


    // Example 2: We use @Replicate.Apply to only include the DATA variant
    // Only the DATA variant will be serialized.
    @Replicate.Model(variants = [DtoVariant.DATA, DtoVariant.PATCH])
    @Replicate.Apply([Serializable::class], include = [DtoVariant.DATA])
    private interface V2 : UserAccount {
        val id: Int
        val startTime: Instant
        val midTime: Instant
        val endTime: List<List<Instant>>
    }

    // Example 3: We use @Replicate.Apply to only exclude the DATA variant
    // All variants will be serialized EXCEPT the DATA variant.
    @Replicate.Model(variants = [DtoVariant.DATA, DtoVariant.CREATE, DtoVariant.PATCH])
    @Replicate.Apply([Serializable::class], exclude = [DtoVariant.DATA])
    private interface V3 : UserAccount {
        val id: Int
        val startTime: Instant
        val midTime: Instant
        val endTime: List<List<Instant>>
    }
}