package io.availe.demo.playground

import io.availe.Replicate
import io.availe.models.DtoVariant

private interface Account {
    @Replicate.Model(variants = [DtoVariant.DATA])
    private interface V1 : Account {
        val id: Int
    }

    @Replicate.Model(variants = [DtoVariant.DATA])
    @Replicate.SchemaVersion(2)
    private interface CurrentAccount : Account {
        val id: Int
        val email: String
    }
}