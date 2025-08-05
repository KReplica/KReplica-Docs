@Replicate.Model(variants = [DATA, CREATE, PATCH])
private interface <SchemaName> {
    @Replicate.Property(exclude = [CREATE])
    val id: <IdType>
    val name: String
}