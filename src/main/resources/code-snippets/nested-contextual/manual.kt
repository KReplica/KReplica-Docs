@Replicate.Model(variants = [DtoVariant.DATA, DtoVariant.CREATE, DtoVariant.PATCH])
private interface AdminAccount {

    @Replicate.Property(include = [DtoVariant.DATA])
    val userForData: UserAccountSchema.V1.Data

    @Replicate.Property(include = [DtoVariant.CREATE])
    val userForCreate: UserAccountSchema.V1.CreateRequest

    @Replicate.Property(include = [DtoVariant.PATCH])
    val userForPatch: UserAccountSchema.V1.PatchRequest
}