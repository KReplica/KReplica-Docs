// Given the UserAccount schema from the homepage...

@Replicate.Model(variants = [DtoVariant.CREATE])
private interface AdminAccount {
    val permissions: List<String>
    val user: UserAccount // Nest the entire UserAccount schema
}

// KReplica will generate this CreateRequest:
public data class CreateRequest(
    public val permissions: List<String>,
    // It automatically uses the UserAccount's CreateRequest,
    // but for a specific version.
    public val user: UserAccountSchema.V1.CreateRequest
) : CreateRequestVariant, KReplicaCreateVariant<AdminAccount>