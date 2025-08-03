package io.availe.demo.playground

import io.availe.Replicate
import io.availe.models.DtoVariant

// --- Direct Annotation Application ---
// Standard annotations like @Deprecated can be applied directly to the interface.
// KReplica will copy them to the generated DTOs.
@Replicate.Model(variants = [DtoVariant.DATA])
@Deprecated("Use NewUserAccount instead")
private interface LegacyAccount {
    @Deprecated("Use newId instead")
    val id: Int
}

// --- Manually Specify Version Number ---
// If you do not wish to use the V<number> naming convention for versions...
private interface Product

// ...you can use @Replicate.SchemaVersion to manually specify the version number.
@Replicate.Model(variants = [DtoVariant.DATA])
@Replicate.SchemaVersion(1)
private interface InitialProduct : Product {
    val sku: String
}