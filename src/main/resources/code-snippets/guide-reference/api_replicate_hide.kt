package io.availe.demo.playground

import io.availe.Replicate
import io.availe.models.DtoVariant

@Replicate.Model(variants = [DtoVariant.DATA])
@Replicate.Hide
private interface InternalFeature {
    val featureId: Int
}

@Replicate.Model(variants = [DtoVariant.DATA])
private interface PublicFeature {
    val id: Int
}