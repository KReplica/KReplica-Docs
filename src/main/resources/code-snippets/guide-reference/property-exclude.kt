@Replicate.Model(variants = [DtoVariant.DATA, DtoVariant.CREATE])
private interface UserProfile {
    @Replicate.Property(exclude = [DtoVariant.CREATE])
    val id: java.util.UUID
    val name: String
}