package io.kreplica.docs.web

object SentinelOobFragment {
    fun start(jobId: String) =
        """<div id="job-$jobId-start" hx-swap-oob="beforeend" hx-trigger="job-start"></div>"""

    fun done(jobId: String) =
        """<div id="job-$jobId-done" hx-swap-oob="beforeend" hx-trigger="job-done"></div>"""
}
