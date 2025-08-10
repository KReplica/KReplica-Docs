package io.availe.demo.playground

import io.availe.Replicate
import io.availe.models.DtoVariant

// Here we apply the deprecated annotation at the model-level.

@Replicate.Model(variants = [DtoVariant.DATA, DtoVariant.CREATE, DtoVariant.PATCH])
@Deprecated(
    message = "LegacyAccount is deprecated. Use NewUserAccount instead.",
    replaceWith = ReplaceWith(
        "io.availe.demo.playground.NewUserAccount(...)",
        imports = ["io.availe.demo.playground.NewUserAccount"]
    ),
    level = DeprecationLevel.WARNING
)
private interface LegacyAccount {
    val id: Int
}

// Here we apply the deprecated annotation at the property-level.

@Replicate.Model(variants = [DtoVariant.DATA, DtoVariant.CREATE, DtoVariant.PATCH])
private interface NewUserAccount {
    @Deprecated("For the sake of this example, this property has been deprecated.")
    val newId: String
}
