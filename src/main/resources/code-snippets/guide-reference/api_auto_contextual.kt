package io.availe.demo.playground

import io.availe.Replicate
import io.availe.models.AutoContextual
import io.availe.models.DtoVariant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.Instant

@Replicate.Model(
    variants = [DtoVariant.DATA],
    autoContextual = AutoContextual.ENABLED
)
@Replicate.Apply([Serializable::class])
private interface Event {
    val id: Int
    val timestamp: Instant

    @Replicate.Property(autoContextual = AutoContextual.DISABLED)
    @Contextual
    val manualTimestamp: Instant
}