package io.availe.kreplicadocs.model.view

import com.fasterxml.jackson.annotation.JsonProperty

data class GuideExampleStub(
    @param:JsonProperty("templateSlug") val templateSlug: String
)

data class GuideSubsectionStub(
    @param:JsonProperty("id") val id: String,
    @param:JsonProperty("exampleSlug") val exampleSlug: String? = null,
    @param:JsonProperty("useTabsKey") val useTabsKey: String? = null
)

data class GuideSectionStub(
    @param:JsonProperty("id") val id: String,
    @param:JsonProperty("subsections") val subsections: List<GuideSubsectionStub> = emptyList()
)