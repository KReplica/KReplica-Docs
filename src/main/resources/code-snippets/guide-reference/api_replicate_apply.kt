package io.availe.demo.playground

import io.availe.Replicate
import io.availe.models.DtoVariant
import kotlinx.serialization.Serializable

@Replicate.Model(variants = [DtoVariant.DATA])
@Replicate.Apply([Serializable::class])
private interface Product {
    val id: Int
    val name: String
}