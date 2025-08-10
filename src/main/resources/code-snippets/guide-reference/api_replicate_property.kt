package io.availe.demo.playground

import io.availe.Replicate
import io.availe.models.DtoVariant
import java.util.UUID

@Replicate.Model(variants = [DtoVariant.DATA, DtoVariant.CREATE])
private interface UserAccount {
    @Replicate.Property(exclude = [DtoVariant.CREATE])
    val id: UUID

    val email: String
}