package io.availe.kreplicadocs.common

enum class PageId {
    INDEX,
    GUIDE,
    PLAYGROUND
}

object WebApp {
    object Endpoints {
        object Pages {
            const val INDEX = "/"
            const val GUIDE = "/guide"
            const val PLAYGROUND = "/playground"
        }

        object Playground {
            const val TEMPLATE_SWAP = "/playground/templates"
            const val COMPILE = "/playground/compile"
        }

        object Examples {
            private const val BASE = "/examples/{slug}"
            const val PLAYGROUND_FILE_SWAP = "$BASE/playground-file-swap/{fileName}"
            const val GENERATED_PANEL = "$BASE/generated-panel/{fileName}"
            const val FILE_CONTENT = "$BASE/file/{fileName}"
            const val FILE_CONTENT_ONLY = "$BASE/file-content/{fileName}"
        }

        object Api {
            const val COMPLETIONS = "/api/completions"
        }
    }

    object ViewModelAttributes {
        const val NAV_LINKS = "navLinks"
        const val CURRENT_PAGE = "currentPage"
        const val ALL_EXAMPLES = "allExamples"
        const val EXAMPLE = "example"
        const val FEATURE_EXAMPLE = "featureExample"
        const val ACTIVE_SLUG = "activeSlug"
        const val ACTIVE_FILE = "activeFile"
        const val LANGUAGE = "language"
        const val CODE = "code"
        const val PROPERTIES = "properties"
        const val TOUR_SELECT_OPTIONS = "tourSelectOptions"
        const val EXAMPLE_SELECT_OPTIONS = "exampleSelectOptions"
    }
}