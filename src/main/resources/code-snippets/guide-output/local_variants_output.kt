public sealed interface <SchemaName>Schema {
    public sealed interface DataVariant : <SchemaName>Schema
    public sealed interface CreateRequestVariant : <SchemaName>Schema
    public sealed interface PatchRequestVariant : <SchemaName>Schema

    public sealed interface V1 : <SchemaName>Schema {
        // V1.Data implements the local variant
        public data class Data(...) : V1, DataVariant, ...
        public data class CreateRequest(...) : V1, CreateRequestVariant, ...
    }

    public sealed interface V2 : <SchemaName>Schema {
        // V2.Data also implements the same local variant
        public data class Data(...) : V2, DataVariant, ...
        public data class PatchRequest(...) : V2, PatchRequestVariant, ...
    }
}