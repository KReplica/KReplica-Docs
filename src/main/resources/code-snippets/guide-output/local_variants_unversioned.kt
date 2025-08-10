public sealed interface <SchemaName>Schema {
    // Local variant markers
    public sealed interface DataVariant : <SchemaName>Schema
    public sealed interface CreateRequestVariant : <SchemaName>Schema
    public sealed interface PatchRequestVariant : <SchemaName>Schema

    // Generated DTOs
    public data class Data(...) : <SchemaName>Schema, DataVariant, ...
    public data class CreateRequest(...) : <SchemaName>Schema, CreateRequestVariant, ...
    public data class PatchRequest(...) : <SchemaName>Schema, PatchRequestVariant, ...
}
