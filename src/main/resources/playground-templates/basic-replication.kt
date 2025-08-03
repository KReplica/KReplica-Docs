package io.availe.demo.playground

import io.availe.Replicate
import io.availe.models.DtoVariant
import java.util.UUID

@Replicate.Model(variants = [DtoVariant.DATA, DtoVariant.CREATE, DtoVariant.PATCH])
private interface UserProfile {
    @Replicate.Property(exclude = [DtoVariant.CREATE])
    val id: UUID

    val username: String

    val email: String

    @Replicate.Property(include = [DtoVariant.DATA])
    val registrationDate: String
}