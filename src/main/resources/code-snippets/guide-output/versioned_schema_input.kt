private interface <SchemaName> {
    @Replicate.Model(variants = [DATA, CREATE])
    private interface V1 : <SchemaName> {
        val id: Int
        val name: String
    }

    @Replicate.Model(variants = [DATA, PATCH])
    private interface V2 : <SchemaName> {
        val id: Int
        val email: String
    }
}