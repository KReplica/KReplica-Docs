@Replicate.Model(
    variants = [DtoVariant.DATA, DtoVariant.CREATE, DtoVariant.PATCH]
)
private interface UserProfile {
    val id: Int
    val name: String
}