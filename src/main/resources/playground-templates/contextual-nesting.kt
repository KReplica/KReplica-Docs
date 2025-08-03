package io.availe.demo.playground

import io.availe.Replicate
import io.availe.models.DtoVariant

private interface UserAccount

@Replicate.Model(variants = [DtoVariant.DATA, DtoVariant.CREATE, DtoVariant.PATCH])
private interface V1 : UserAccount {
    val id: Int
}

@Replicate.Model(variants = [DtoVariant.DATA, DtoVariant.CREATE, DtoVariant.PATCH])
private interface AdminAccount {
    val user: V1
    val permissions: List<String>
}