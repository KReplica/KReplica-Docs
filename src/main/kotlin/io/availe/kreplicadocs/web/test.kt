//package io.availe.kreplicadocs.web
//
//import io.availe.Replicate
//import io.availe.models.DtoVariant
//import java.io.Serializable
//import kotlin.time.ExperimentalTime
//import kotlin.time.Instant
//import kotlin.uuid.ExperimentalUuidApi
//import kotlin.uuid.Uuid
//
//@OptIn(ExperimentalUuidApi::class)
//@Replicate.Apply([Serializable::class])
//@Replicate.Model(variants = [DtoVariant.DATA, DtoVariant.CREATE, DtoVariant.PATCH])
//private interface UserAccount {
//    @Replicate.Property(include = [DtoVariant.DATA])
//    val id: Uuid
//    val email: String
//
//    @OptIn(ExperimentalTime::class)
//    val devices: List<Pair<DeviceName, Instant>>
//
//    @Replicate.Property(exclude = [DtoVariant.CREATE])
//    val banReason: String?
//}
//
//@JvmInline
//value class DeviceName(val value: String)