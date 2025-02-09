package com.reposcorer.service

import com.reposcorer.api.GitHubClient
import com.reposcorer.model.SearchResponse
import com.reposcorer.scoring.RepoScorer
import com.reposcorer.util.createTestRepo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RepoScorerServiceTest {
    private val mockGithubClient = mockk<GitHubClient>()
    private val repoScorer = RepoScorer()
    private val service = RepoScorerService(mockGithubClient, repoScorer)

    @Test
    fun `returns sorted repositories`() = runTest {
        // Given
        val testRepos = listOf(
            createTestRepo(stars = 1000, forks = 500),
            createTestRepo(stars = 100, forks = 50),
            createTestRepo(stars = 10, forks = 5)
        )
        val searchResponse = SearchResponse(3, false, testRepos)

        coEvery {
            mockGithubClient.searchRepositories(
                language = any(),
                createdDate = any(),
                maxRetries = any(),
                initialDelayMs = any()
            )
        } returns Result.success(searchResponse)

        // When
        val result = service.getTopRepositories("kotlin", LocalDate.now(), 2)

        // Then
        assertTrue(result.isSuccess)
        result.onSuccess { repos ->
            assertEquals(2, repos.size, "Should return only top 2 repositories")
            assertTrue(repos[0].score > repos[1].score, "Should have repositories sorted by score")
        }

        coVerify {
            mockGithubClient.searchRepositories(any(), any(), any(), any())
        }
    }

    @Test
    fun `handles empty response when getting repositories`() = runTest {
        // Given
        val searchResponse = SearchResponse(0, false, emptyList())
        coEvery {
            mockGithubClient.searchRepositories(
                language = any(),
                createdDate = any(),
                maxRetries = any(),
                initialDelayMs = any()
            )
        } returns Result.success(searchResponse)

        // When
        val result = service.getTopRepositories("kotlin", LocalDate.now())

        // Then
        assertTrue(result.isSuccess)
        result.onSuccess { repos ->
            assertTrue(repos.isEmpty(), "Should return empty list when no repositories found")
        }

        coVerify {
            mockGithubClient.searchRepositories(any(), any(), any(), any())
        }
    }

    @Test
    fun `handles failure when getting repositories`() = runTest {
        // Given
        val error = RuntimeException("API Error")
        coEvery {
            mockGithubClient.searchRepositories(
                language = any(),
                createdDate = any(),
                maxRetries = any(),
                initialDelayMs = any()
            )
        } returns Result.failure(error)

        // When
        val result = service.getTopRepositories("kotlin", LocalDate.now())

        // Then
        assertFalse(result.isSuccess)
        assertEquals(error, result.exceptionOrNull())

        coVerify {
            mockGithubClient.searchRepositories(any(), any(), any(), any())
        }
    }
}