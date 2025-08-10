package io.availe.demo.playground

import io.availe.Replicate
import io.availe.models.DtoVariant

@Replicate.Model(variants = [DtoVariant.DATA, DtoVariant.CREATE])
private interface UserProfile {
    @Replicate.Property(exclude = [DtoVariant.CREATE])
    val id: java.util.UUID
    val name: String
}