import io.availe.Replicate
import io.availe.models.DtoVariant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Replicate.Model(variants = [DtoVariant.DATA, DtoVariant.CREATE, DtoVariant.PATCH])
private interface UserAccount {
    @Replicate.Property(exclude = [DtoVariant.CREATE])
    val id: Uuid
    val email: String
    val displayName: String?
}