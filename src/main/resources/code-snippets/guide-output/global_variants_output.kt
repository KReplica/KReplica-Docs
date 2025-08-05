// From an unversioned schema
public data class Data(...) :
    <SchemaName>Schema,
KReplicaDataVariant<<SchemaName>Schema>

// From a versioned schema
public data class Data(...) :
    V1,
    DataVariant,
    KReplicaDataVariant<V1>