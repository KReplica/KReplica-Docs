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

    // Notice how all the above examples follow the `V<number> : UserAccount` naming scheme.
    // If you wish to provide a custom name, you can do it via the @SchemaVersion annotation.
    // The @SchemaVersion annotation takes an Int as a parameter; let's make it Version 3.
    @Replicate.Model(variants = [DtoVariant.DATA, DtoVariant.PATCH])
    @Replicate.SchemaVersion(3)
    private interface NewModel : UserAccount {
        val id: Int
        val username: String
        val email: String
    }
}