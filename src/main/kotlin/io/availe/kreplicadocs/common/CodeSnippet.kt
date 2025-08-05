package io.availe.kreplicadocs.common

enum class CodeSnippet(val path: String) {
    QUICKSTART_BUILD("general/quickstart-build.kts"),
    PROPERTY_EXAMPLE("general/property-example.kt"),

    NESTING_AUTOMATIC("nested-contextual/automatic.kt"),
    NESTING_MANUAL("nested-contextual/manual.kt"),

    WHEN_EXPRESSION_ALL("when-expressions/all.kt"),
    WHEN_EXPRESSION_BY_VARIANT("when-expressions/by-variant.kt"),
    WHEN_EXPRESSION_BY_VERSION("when-expressions/by-version.kt"),

    MAPPER_PATTERN_INTERFACE("mapper-pattern/01-generic-interface.kt"),
    MAPPER_PATTERN_DOMAIN_MODEL("mapper-pattern/02-domain-model.kt"),
    MAPPER_PATTERN_KREPLICA_INTERFACE("mapper-pattern/03-kreplica-interface.kt"),
    MAPPER_PATTERN_IMPLEMENTATION("mapper-pattern/04-implementation.kt"),

    HOMEPAGE_DEMO_SOURCE("homepage/demo-source.kt"),

    GUIDE_REF_MODEL_VARIANTS("guide-reference/model-variants.kt"),
    GUIDE_REF_PROPERTY_EXCLUDE("guide-reference/property-exclude.kt"),
    GUIDE_REF_VERSIONING("guide-reference/versioning.kt"),
    GUIDE_REF_NESTING_UNVERSIONED("guide-reference/nesting-unversioned.kt"),
    GUIDE_REF_NESTING_VERSIONED("guide-reference/nesting-versioned.kt"),

    API_REPLICATE_MODEL("guide-reference/api_replicate_model.kt"),
    API_REPLICATE_PROPERTY("guide-reference/api_replicate_property.kt"),
    API_REPLICATE_APPLY("guide-reference/api_replicate_apply.kt"),
    API_REPLICATE_SCHEMA_VERSION("guide-reference/api_replicate_schema_version.kt"),
    API_REPLICATE_HIDE("guide-reference/api_replicate_hide.kt"),
    API_AUTO_CONTEXTUAL("guide-reference/api_auto_contextual.kt"),
}