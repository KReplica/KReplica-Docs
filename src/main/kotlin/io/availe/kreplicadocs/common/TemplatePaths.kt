package io.availe.kreplicadocs.common

enum class PageTemplate(val path: String) {
    INDEX("pages/index"),
    GUIDE("pages/guide"),
    PLAYGROUND("pages/playground")
}

enum class PartialTemplate(val path: String) {
    CONTENT_INDEX("partials/content-index"),
    CONTENT_GUIDE("partials/content-guide"),
    CONTENT_PLAYGROUND("partials/content-playground")
}

enum class FragmentTemplate(val path: String) {
    NAV_UPDATE_OOB("fragments/nav-update-oob"),
    FAB_UPDATE_OOB("fragments/fab-update-oob"),
    PLAYGROUND_EDITOR_SWAP("fragments/playground-editor-swap"),
    PLAYGROUND_COMPILING("fragments/playground-compiling"),
    PLAYGROUND_RESULTS("fragments/playground-results"),
    PLAYGROUND_ERROR("fragments/playground-error")
}

enum class GuideContentTemplate(val path: String) {
    API_ANNOTATIONS_REPLICATE_APPLY("guide/content/api-annotations/replicate-apply"),
    API_ANNOTATIONS_REPLICATE_HIDE("guide/content/api-annotations/replicate-hide"),
    API_ANNOTATIONS_REPLICATE_MODEL("guide/content/api-annotations/replicate-model"),
    API_ANNOTATIONS_REPLICATE_PROPERTY("guide/content/api-annotations/replicate-property"),
    API_ANNOTATIONS_REPLICATE_SCHEMA_VERSION("guide/content/api-annotations/replicate-schema-version"),
    API_CONCEPTS_APPLYING_ANNOTATIONS("guide/content/api-concepts/applying-annotations"),
    API_CONCEPTS_AUTO_CONTEXTUALIZATION("guide/content/api-concepts/auto-contextualization"),
    API_CONCEPTS_DTO_VERSIONING("guide/content/api-concepts/dto-versioning"),
    API_CONCEPTS_NOMINAL_TYPING("guide/content/api-concepts/nominal-typing"),
    FAQ_BROADER_REPLICATION("guide/content/faq/broader-replication"),
    FAQ_COMPILATION_ORDER("guide/content/faq/compilation-order"),
    FAQ_PRIVATE_KEYWORD("guide/content/faq/private-keyword"),
    GENERATED_CODE_GLOBAL_VARIANTS("guide/content/generated-code/generated_code_global_variants"),
    GENERATED_CODE_LOCAL_VARIANTS("guide/content/generated-code/generated_code_local_variants"),
    GENERATED_CODE_PATCHABLE("guide/content/generated-code/generated_code_patchable"),
    GENERATED_CODE_SCHEMAS("guide/content/generated-code/generated_code_schemas"),
    INTRODUCTION_PREFACE("guide/content/introduction/preface"),
    INTRODUCTION_SETUP("guide/content/introduction/setup"),
    INTRODUCTION_STABILITY("guide/content/introduction/stability"),
    PATTERNS_API_MAPPERS("guide/content/patterns/patterns_api_mappers"),
    PATTERNS_EXHAUSTIVE_WHEN_STATEMENTS("guide/content/patterns/exhaustive_when_statements"),
    PATTERNS_KOTLINX_SERIALIZATION("guide/content/patterns/patterns_kotlinx_serialization"),
}