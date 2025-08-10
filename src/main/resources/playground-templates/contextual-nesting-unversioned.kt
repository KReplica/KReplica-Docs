package io.availe.demo.playground

import io.availe.Replicate
import io.availe.models.DtoVariant

/*
Here, we wish to include an unversioned UserAccount as a property of AdminAccount.
However, we want the following structure:

AdminAccount DATA variant should include UserAccount DATA variant
AdminAccount CREATE variant should include UserAccount CREATE variant
AdminAccount PATCH variant should include UserAccount PATCH variant

We can do this manually with include/exclude property modifiers, but
that is a lot of boilerplate. Instead, we can just list the interface
UserAccount as a property of AdminAccount.
*/

@Replicate.Model(variants = [DtoVariant.DATA, DtoVariant.CREATE, DtoVariant.PATCH])
private interface UserAccount {
    val id: Int
}

// Here, we define AdminAccount to include the UserAccount interface as a property.
@Replicate.Model(variants = [DtoVariant.DATA, DtoVariant.CREATE, DtoVariant.PATCH])
private interface AdminAccount {
    val user: UserAccountSchema
}