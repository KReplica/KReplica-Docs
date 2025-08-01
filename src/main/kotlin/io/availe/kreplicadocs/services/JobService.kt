package io.availe.kreplicadocs.services

interface JobService {
    fun consumeFragments(jobId: String, consumer: (String) -> Unit)
}