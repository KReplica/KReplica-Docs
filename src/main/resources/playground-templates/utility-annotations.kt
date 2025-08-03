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