package io.availe.demo.playground

import io.availe.Replicate
import io.availe.models.DtoVariant

private interface UserAccount {
    @Replicate.Model(variants = [DtoVariant.DATA])
    private interface V1 : UserAccount {
        val id: Int
    }

    @Replicate.Model(variants = [DtoVariant.DATA, DtoVariant.PATCH])
    private interface V2 : UserAccount {
        val id: Int
        val email: String
    }

    // manually assign version number
    @Replicate.Model(variants = [DtoVariant.DATA, DtoVariant.PATCH])
    @Replicate.SchemaVersion(3)
    private interface NewModel : UserAccount {
        val id: Int
        val username: String
        val email: String
    }
}