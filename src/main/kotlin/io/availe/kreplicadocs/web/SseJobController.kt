package io.kreplica.docs.web

import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.Duration

@RestController
class SseJobController(private val jobService: JobService) {

    @GetMapping("/api/jobs/{jobId}/stream")
    fun stream(@PathVariable jobId: String): Flux<ServerSentEvent<String>> {
        val start = ServerSentEvent.builder<String>()
            .id("$jobId-start")
            .event("message")
            .data(SentinelOobFragment.start(jobId))
            .build()

        val body = jobService.stream(jobId)
            .map { html ->
                ServerSentEvent.builder<String>()
                    .id(jobId)
                    .event("message")
                    .data(html)
                    .build()
            }

        val done = jobService.final(jobId)
            .map { html -> html + SentinelOobFragment.done(jobId) }
            .map { full ->
                ServerSentEvent.builder<String>()
                    .id("$jobId-done")
                    .event("message")
                    .data(full)
                    .build()
            }

        return Flux.concat(Flux.just(start), body, done)
            .timeout(Duration.ofMinutes(10))
    }
}
