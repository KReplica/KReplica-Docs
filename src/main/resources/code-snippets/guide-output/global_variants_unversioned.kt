public sealed interface <SchemaName>Schema {
    // Local variant: for matching all Data in this schema
    public sealed interface DataVariant : <SchemaName>Schema

    // Data DTO implements both the local variant and the global variant (KReplicaDataVariant)
    public data class Data(...) : <SchemaName>Schema, DataVariant, KReplicaDataVariant<<SchemaName>Schema>
}