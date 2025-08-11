package io.availe.demo

import io.availe.Replicate
import io.availe.models.DtoVariant
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
@Replicate.Apply([Serializable::class])
@Replicate.Model(variants = [DtoVariant.DATA, DtoVariant.CREATE, DtoVariant.PATCH])
private interface UserAccount {
    @Replicate.Property(include = [DtoVariant.DATA])
    val id: Uuid

    val email: String
    val lastLogin: Instant
    val sessionTokens: SessionTokens
}

// KReplica is flexible, you can use whatever types you wish
@JvmInline
value class SessionTokens(val value: List<String>)