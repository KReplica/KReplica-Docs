private interface UserAccount {
    @Replicate.Model(variants = [DtoVariant.CREATE])
    interface V1 : UserAccount {
        val email: String
    }
}

@Replicate.Model(variants = [DtoVariant.CREATE])
private interface AdminAccount {
    val permissions: List<String>
    val user: UserAccountSchema.V1
}