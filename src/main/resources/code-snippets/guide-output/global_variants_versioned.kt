public sealed interface <SchemaName>Schema {
    // Local variant: for matching all Data in this schema (across versions)
    public sealed interface DataVariant : <SchemaName>Schema

    public sealed interface V1 : <SchemaName>Schema {
        // Data DTO for V1 implements the local variant and the global variant (KReplicaDataVariant)
        public data class Data(...) : V1, DataVariant, KReplicaDataVariant<V1>
    }
    public sealed interface V2 : <SchemaName>Schema {
        // Data DTO for V2 also implements the local variant and the global variant (KReplicaDataVariant)
        public data class Data(...) : V2, DataVariant, KReplicaDataVariant<V2>
    }
    // ...more versions as needed
}