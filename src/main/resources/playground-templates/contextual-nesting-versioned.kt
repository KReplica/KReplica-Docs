package io.availe.demo.playground

import io.availe.Replicate
import io.availe.models.DtoVariant

/*
This is basically the same thing as (unversioned) contextual nesting.
Except that instead of putting "UserAccountSchema," you put "UserAccountSchema.V1"
*/

private interface UserAccount {
    @Replicate.Model(variants = [DtoVariant.DATA, DtoVariant.CREATE, DtoVariant.PATCH])
    private interface V1 : UserAccount {
        val id: Int
    }
}

// Here, we define AdminAccount to include the UserAccountSchema.V1 interface as a property.
@Replicate.Model(variants = [DtoVariant.DATA, DtoVariant.CREATE, DtoVariant.PATCH])
private interface AdminAccount {
    val user: UserAccountSchema.V1
}