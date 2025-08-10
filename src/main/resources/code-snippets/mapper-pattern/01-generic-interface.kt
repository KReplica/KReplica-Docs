import io.availe.models.KReplicaCreateVariant
import io.availe.models.KReplicaDataVariant
import io.availe.models.KReplicaPatchVariant

interface ApiSchemaMapper<
        M,
        ID,
        V : Any,
        D : KReplicaDataVariant<V>,
        C : KReplicaCreateVariant<V>,
        P : KReplicaPatchVariant<V>
        > {
    fun toDataDto(model: M): D
    fun toDomain(id: ID, dto: C): M
    fun applyPatch(model: M, patch: P): M
}