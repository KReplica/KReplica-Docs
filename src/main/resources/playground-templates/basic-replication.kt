package io.availe.demo.playground

import io.availe.Replicate
import io.availe.models.DtoVariant
import java.util.UUID

// The @Replicate.Model annotation requests DATA, CREATE, and PATCH variants for this interface.
@Replicate.Model(variants = [DtoVariant.DATA, DtoVariant.CREATE, DtoVariant.PATCH])
private interface UserProfile {
    // We specify that `id` will ONLY be created in the DATA variant.
    // By only allowing `id` in the DATA variant, we effectively make it read only.
    // A common use case for this is to make `id` managed by the database.
    @Replicate.Property(include = [DtoVariant.DATA])
    val id: UUID

    // These properties have no override, so they will be created
    // in all the variants defined in @Replicate.Model (DATA, PATCH, CREATE)
    val username: String
    val email: String

    // This property is excluded from the CREATE variant.
    // We thus avoid contaminating our CREATE variant with unneeded fields.
    @Replicate.Property(exclude = [DtoVariant.CREATE])
    val banReason: String
}