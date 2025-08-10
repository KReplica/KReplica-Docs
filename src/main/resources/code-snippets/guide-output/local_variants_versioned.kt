public sealed interface <SchemaName>Schema {
    // Local variant markers for all versions
    public sealed interface DataVariant : <SchemaName>Schema
    public sealed interface CreateRequestVariant : <SchemaName>Schema
    public sealed interface PatchRequestVariant : <SchemaName>Schema

    public sealed interface V1 : <SchemaName>Schema {
        // V1 DTOs implement local variants
        public data class Data(...) : V1, DataVariant, ...
        public data class CreateRequest(...) : V1, CreateRequestVariant, ...
    }
    public sealed interface V2 : <SchemaName>Schema {
        // V2 DTOs also implement local variants
        public data class Data(...) : V2, DataVariant, ...
        public data class PatchRequest(...) : V2, PatchRequestVariant, ...
    }
}