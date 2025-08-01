package io.availe.kreplicadocs.api

import io.availe.kreplicadocs.services.JobService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ExecutorService

@RestController
@RequestMapping("/api/jobs")
class JobStreamController(
    private val jobService: JobService,
    private val virtualThreadExecutor: ExecutorService
) {

    @GetMapping("/{jobId}/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun stream(@PathVariable jobId: String): SseEmitter {
        val emitter = SseEmitter(0L)
        virtualThreadExecutor.submit {
            try {
                emitter.send(
                    """<template hx-swap-oob="true" data-type="sentinel" data-phase="start" data-job-id="$jobId"></template>"""
                )
                jobService.consumeFragments(jobId) { fragment ->
                    emitter.send(fragment)
                }
                emitter.send(
                    """<template hx-swap-oob="true" data-type="sentinel" data-phase="done" data-job-id="$jobId"></template>"""
                )
                emitter.complete()
            } catch (ex: Exception) {
                emitter.completeWithError(ex)
            }
        }
        return emitter
    }
}
