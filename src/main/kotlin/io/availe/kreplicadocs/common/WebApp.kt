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
    }
}