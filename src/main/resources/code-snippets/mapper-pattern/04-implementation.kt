package io.availe.demo.mapping

import io.availe.demo.domain.UserModel
import io.availe.demo.models.UserAccountSchema
import io.availe.models.Patchable
import java.util.UUID

object UserV1Mapper : ApiSchemaMapper<
        UserModel,
        UUID,
        UserAccountSchema.V1,
        UserAccountSchema.V1.Data,
        UserAccountSchema.V1.CreateRequest,
        UserAccountSchema.V1.PatchRequest,
        > {
    override fun toDataDto(model: UserModel): UserAccountSchema.V1.Data =
        UserAccountSchema.V1.Data(
            id = model.id,
            firstName = model.firstName,
            lastName = model.lastName,
            email = model.email,
        )

    override fun toDomain(id: UUID, dto: UserAccountSchema.V1.CreateRequest): UserModel =
        UserModel(
            id = id,
            firstName = dto.firstName.trim(),
            lastName = dto.lastName.trim(),
            email = dto.email.trim(),
        )

    override fun applyPatch(model: UserModel, patch: UserAccountSchema.V1.PatchRequest): UserModel =
        model.copy(
            firstName = (patch.firstName as? Patchable.Set)?.value?.trim() ?: model.firstName,
            lastName = (patch.lastName as? Patchable.Set)?.value?.trim() ?: model.lastName,
            email = (patch.email as? Patchable.Set)?.value?.trim() ?: model.email,
        )
}