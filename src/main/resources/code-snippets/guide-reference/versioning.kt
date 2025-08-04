private interface UserAccount {
    @Replicate.Model(variants = [DtoVariant.DATA])
    private interface V1 : UserAccount {
        val id: Int
    }

    @Replicate.Model(variants = [DtoVariant.DATA, DtoVariant.PATCH])
    private interface V2 : UserAccount {
        val id: Int
        val email: String
    }
}