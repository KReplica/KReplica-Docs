fun handleV2Variants(user: UserAccountSchema.V2) {
    when (user) {
        is UserAccountSchema.V2.CreateRequest -> println("Handle V2 Create: ${user.email}")
        is UserAccountSchema.V2.Data -> println("Handle V2 Data: ${user.id}")
        is UserAccountSchema.V2.PatchRequest -> println("Handle V2 Patch")
    }
}