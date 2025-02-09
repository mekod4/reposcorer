package com.reposcorer.api

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.reposcorer.config.AppConfig
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GitHubClientIntegrationTest {
    private lateinit var wireMockServer: WireMockServer
    private lateinit var client: GitHubClient

    @Before
    fun setup() {
        wireMockServer = WireMockServer(wireMockConfig().dynamicPort())
        wireMockServer.start()

        val testConfig = object : AppConfig() {
            override val baseUrl: String = wireMockServer.baseUrl()
        }
        client = GitHubClient(testConfig)
    }

    @After
    fun teardown() {
        wireMockServer.stop()
    }

    @Test
    fun `extracts results from multiple pages correctly`() = runTest {
        // Given
        val page1Response = createSearchResponse(
            3, listOf(
                createRepoJson(1, "repo1", 100), createRepoJson(2, "repo2", 90)
            )
        )
        val page2Response = createSearchResponse(
            3, listOf(
                createRepoJson(3, "repo3", 80)
            )
        )

        wireMockServer.stubFor(
            get(urlPathEqualTo("/search/repositories")).withQueryParam("page", equalTo("1")).willReturn(
                    aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(page1Response)
                )
        )

        wireMockServer.stubFor(
            get(urlPathEqualTo("/search/repositories")).withQueryParam("page", equalTo("2")).willReturn(
                    aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(page2Response)
                )
        )

        // When
        val result = client.searchRepositories(
            language = "kotlin", createdDate = LocalDate.now().minusMonths(1).toString()
        )

        // Then
        assertTrue(result.isSuccess)
        result.onSuccess { response ->
            assertEquals(3, response.totalCount)
            assertEquals(3, response.items.size, "Should have all items from both pages")
            assertEquals("repo1", response.items[0].name)
            assertEquals("repo3", response.items[2].name)
        }
    }

    @Test
    fun `handles rate limiting between pages`() = runTest {
        // Given
        val page1Response = createSearchResponse(
            2, listOf(
                createRepoJson(1, "repo1", 100)
            )
        )
        val rateLimitResponse = """
            {
                "message": "API rate limit exceeded",
                "documentation_url": "https://docs.github.com/rest/overview/resources-in-the-rest-api#rate-limiting"
            }
        """.trimIndent()
        val page2Response = createSearchResponse(
            2, listOf(
                createRepoJson(2, "repo2", 90)
            )
        )

        wireMockServer.stubFor(
            get(urlPathEqualTo("/search/repositories")).withQueryParam("page", equalTo("1")).willReturn(
                    aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(page1Response)
                )
        )

        wireMockServer.stubFor(
            get(urlPathEqualTo("/search/repositories")).withQueryParam("page", equalTo("2")).inScenario("Rate Limit")
                .whenScenarioStateIs("Started").willReturn(
                    aResponse().withStatus(403).withHeader("Content-Type", "application/json")
                        .withBody(rateLimitResponse)
                ).willSetStateTo("Retry")
        )

        // Mock representing the successful retry of second page
        wireMockServer.stubFor(
            get(urlPathEqualTo("/search/repositories")).withQueryParam("page", equalTo("2")).inScenario("Rate Limit")
                .whenScenarioStateIs("Retry").willReturn(
                    aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(page2Response)
                )
        )

        // When
        val result = client.searchRepositories(
            language = "kotlin", createdDate = LocalDate.now().minusMonths(1).toString()
        )

        // Then
        assertTrue(result.isSuccess)
        result.onSuccess { response ->
            assertEquals(2, response.totalCount)  // Changed from 3 to 2
            assertEquals(2, response.items.size)
            assertEquals("repo1", response.items[0].name)
            assertEquals("repo2", response.items[1].name)
        }
    }

    @Test
    fun `handles empty responses (search with no hits) correctly`() = runTest {
        // Given
        val emptyResponse = createSearchResponse(0, emptyList())

        wireMockServer.stubFor(
            get(urlPathEqualTo("/search/repositories")).willReturn(
                    aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(emptyResponse)
                )
        )

        // When
        val result = client.searchRepositories(
            language = "kotlin", createdDate = LocalDate.now().minusMonths(1).toString()
        )

        // Then
        assertTrue(result.isSuccess)
        result.onSuccess { response ->
            assertEquals(0, response.totalCount)
            assertTrue(response.items.isEmpty())
        }
    }

    @Test
    fun `produces a failure when there is a network timeout`() = runTest {
        // Given
        val page1Response = createSearchResponse(
            2, listOf(
                createRepoJson(1, "repo1", 100)
            )
        )

        wireMockServer.stubFor(
            get(urlPathEqualTo("/search/repositories")).withQueryParam("page", equalTo("1")).willReturn(
                    aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(page1Response)
                )
        )

        wireMockServer.stubFor(
            get(urlPathEqualTo("/search/repositories")).withQueryParam("page", equalTo("2")).willReturn(
                    aResponse().withFixedDelay(3000).withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{}")
                )
        )

        // When
        val result = client.searchRepositories(
            language = "kotlin",
            createdDate = LocalDate.now().minusMonths(1).toString(),
            maxRetries = 1,
            initialDelayMs = 100
        )

        // Then
        assertTrue(result.isFailure)
    }

    private fun createSearchResponse(totalCount: Int, items: List<String>): String = """
        {
            "total_count": $totalCount,
            "incomplete_results": false,
            "items": [${items.joinToString(",")}]
        }
    """.trimIndent()

    private fun createRepoJson(id: Int, name: String, stars: Int) = """
        {
            "id": $id,
            "name": "$name",
            "full_name": "test/$name",
            "stargazers_count": $stars,
            "forks_count": ${stars / 2},
            "updated_at": "2024-02-09T12:00:00Z",
            "created_at": "2024-01-09T12:00:00Z",
            "language": "kotlin"
        }
    """.trimIndent()
}