fun handleAllUserTypes(user: UserAccountSchema) {
    when (user) {
        is UserAccountSchema.V1.CreateRequest -> println("Handle V1 Create")
        is UserAccountSchema.V1.Data -> println("Handle V1 Data")
        is UserAccountSchema.V1.PatchRequest -> println("Handle V1 Patch")
        is UserAccountSchema.V2.CreateRequest -> println("Handle V2 Create")
        is UserAccountSchema.V2.Data -> println("Handle V2 Data")
        is UserAccountSchema.V2.PatchRequest -> println("Handle V2 Patch")
    }
}