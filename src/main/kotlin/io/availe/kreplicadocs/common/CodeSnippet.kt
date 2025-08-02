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

    HOMEPAGE_MICRO_DEMO_BEFORE("homepage/micro-demo-before.kt"),
    HOMEPAGE_MICRO_DEMO_AFTER("homepage/micro-demo-after.kt")
}