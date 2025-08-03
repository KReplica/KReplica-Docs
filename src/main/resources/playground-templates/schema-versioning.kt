package io.availe.demo.playground

import io.availe.Replicate
import io.availe.models.DtoVariant

// Create a base, empty interface to group all versions of a schema.
private interface UserAccount {

    // Version 1 of the schema.
    @Replicate.Model(variants = [DtoVariant.DATA])
    private interface V1 : UserAccount {
        val id: Int
        val username: String
    }

    // Version 2 evolves the schema by adding an `email` field and a `PATCH` variant.
    // KReplica combines these into a single, version-aware `UserAccountSchema` sealed interface.
    @Replicate.Model(variants = [DtoVariant.DATA, DtoVariant.PATCH])
    private interface V2 : UserAccount {
        val id: Int
        val username: String
        val email: String
    }
}