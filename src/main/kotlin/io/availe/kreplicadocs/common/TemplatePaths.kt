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
}