package io.availe.demo.playground

import io.availe.Replicate
import io.availe.models.DtoVariant

@Replicate.Model(
    variants = [DtoVariant.DATA, DtoVariant.CREATE, DtoVariant.PATCH]
)
private interface UserProfile {
    val id: Int
    val name: String
}