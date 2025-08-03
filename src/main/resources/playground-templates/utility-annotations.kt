package io.availe.demo.playground

import io.availe.Replicate
import io.availe.models.DtoVariant

@Replicate.Model(variants = [DtoVariant.DATA])
@Deprecated("Use NewUserAccount instead")
private interface LegacyAccount {
    @Deprecated("Use newId instead")
    val id: Int
}

@Replicate.Model(variants = [DtoVariant.DATA])
@Replicate.Hide
private interface HiddenAccount {
    val id: Int
}

private interface Product

@Replicate.Model(variants = [DtoVariant.DATA])
@Replicate.SchemaVersion(1)
private interface InitialProduct : Product {
    val sku: String
}