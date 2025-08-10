package io.availe.demo.models

import io.availe.Replicate
import io.availe.models.DtoVariant
import java.util.UUID

private interface UserAccount {
    @Replicate.Model(
        variants = [DtoVariant.DATA, DtoVariant.PATCH, DtoVariant.CREATE],
    )
    private interface V1 : UserAccount {
        @Replicate.Property(exclude = [DtoVariant.CREATE])
        val id: UUID
        val firstName: String
        val lastName: String
        val email: String
    }
}