@Replicate.Model(variants = [DtoVariant.CREATE])
private interface UserAccount {
    val email: String
}

@Replicate.Model(variants = [DtoVariant.CREATE])
private interface AdminAccount {
    val permissions: List<String>
    val user: UserAccountSchema
}