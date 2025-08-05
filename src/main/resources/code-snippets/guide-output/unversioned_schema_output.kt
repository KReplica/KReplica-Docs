public sealed interface <SchemaName>Schema {
    public data class Data(
        val id: <IdType>,
        val name: String
    ) : <SchemaName>Schema, KReplicaDataVariant<<SchemaName>Schema>

    public data class CreateRequest(
        val name: String
    ) : <SchemaName>Schema, KReplicaCreateVariant<<SchemaName>Schema>

    public data class PatchRequest(
        val id: Patchable<<IdType>> = Patchable.Unchanged,
    val name: Patchable<String> = Patchable.Unchanged
    ) : <SchemaName>Schema, KReplicaPatchVariant<<SchemaName>Schema>
}