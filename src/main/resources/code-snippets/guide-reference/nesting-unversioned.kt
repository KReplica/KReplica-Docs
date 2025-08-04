package io.availe.demo.playground

import io.availe.Replicate
import io.availe.models.DtoVariant

@Replicate.Model(variants = [DtoVariant.CREATE])
private interface UserAccount {
    val email: String
}

@Replicate.Model(variants = [DtoVariant.CREATE])
private interface AdminAccount {
    val permissions: List<String>
    val user: UserAccountSchema
}