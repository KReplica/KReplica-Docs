package io.availe.demo.patterns

import io.availe.Replicate
import io.availe.models.DtoVariant
import kotlinx.serialization.Serializable

@Replicate.Model(variants = [DtoVariant.DATA, DtoVariant.CREATE])
@Replicate.Apply([Serializable::class])
private interface Product {
    @Replicate.Property(exclude = [DtoVariant.CREATE])
    val id: Int
    val name: String
    val price: Double
}