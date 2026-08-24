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

        object Fragments {
            const val INDEX = "/fragments"
            const val GUIDE = "/fragments/guide"
            const val PLAYGROUND = "/fragments/playground"

            fun forPage(pageId: PageId): String = when (pageId) {
                PageId.INDEX -> INDEX
                PageId.GUIDE -> GUIDE
                PageId.PLAYGROUND -> PLAYGROUND
            }
        }

        object Playground {
            const val TEMPLATE_SWAP = "/playground/templates"
            const val COMPILE = "/playground/compile"
        }
    }
}