package io.availe.demo.playground

import io.availe.Replicate
import io.availe.models.DtoVariant

// The @Replicate.Hide annotation tells KReplica to skip code generation for this model.
// This is useful for temporarily testing the impact of removing a schema.
@Replicate.Model(variants = [DtoVariant.DATA])
@Replicate.Hide
private interface HiddenAccount {
    val id: Int
}