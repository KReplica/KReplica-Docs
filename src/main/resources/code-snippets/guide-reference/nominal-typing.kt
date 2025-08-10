package io.availe.demo.playground

import io.availe.Replicate
import io.availe.models.DtoVariant
import io.availe.models.NominalTyping

@Replicate.Model(variants = [DtoVariant.DATA], nominalTyping = NominalTyping.ENABLED)
private interface Order {
    val id: Long
    val customerId: Long

    @Replicate.Property(nominalTyping = NominalTyping.DISABLED)
    val totalAmount: Double
}