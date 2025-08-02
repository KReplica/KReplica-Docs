package io.availe.demo

import io.availe.Replicate
import io.availe.models.DtoVariant
import java.util.UUID

@Replicate.Model(variants = [DtoVariant.DATA, DtoVariant.CREATE, DtoVariant.PATCH])
private interface UserAccount {
    // ID is excluded from creation requests
    @Replicate.Property(exclude = [DtoVariant.CREATE])
    val id: UUID

    val email: String
    val displayName: String?
}