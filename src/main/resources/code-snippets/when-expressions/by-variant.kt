fun handleAllDataVariants(data: UserAccountSchema.DataVariant) {
    when (data) {
        is UserAccountSchema.V1.Data -> println("Handle V1 Data: ${data.id}")
        is UserAccountSchema.V2.Data -> println("Handle V2 Data: ${data.email}")
    }
}