package com.reposcorer.scoring

import com.reposcorer.util.createTestRepo
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RepoScorerTest {
    private val scorer = RepoScorer()

    @Test
    fun `individual score components evaluate the right weights`() {
        // stars
        val highStarsRepo = createTestRepo(stars = 1000000, forks = 0, daysAgo = 365)
        assertEquals(0.5, scorer.calculateScore(highStarsRepo), "Max stars should contribute exactly its weight")

        // forks
        val highForksRepo = createTestRepo(stars = 0, forks = 100000, daysAgo = 365)
        assertEquals(0.3, scorer.calculateScore(highForksRepo), "Max forks should contribute exactly its weight")

        // recency
        val recentRepo = createTestRepo(stars = 0, forks = 0, daysAgo = 0)
        assertEquals(0.2, scorer.calculateScore(recentRepo), "Maximum recency should contribute exactly its weight")
    }

    @Test
    fun `scoring considers recency`() {
        val recentRepo = createTestRepo(stars = 100, forks = 50, daysAgo = 1)
        val oldRepo = createTestRepo(stars = 100, forks = 50, daysAgo = 300)

        val recentScore = scorer.calculateScore(recentRepo)
        val oldScore = scorer.calculateScore(oldRepo)

        assertTrue(recentScore > oldScore, "Recent repository should score higher")
    }

    @Test
    fun `scoring gives perfect score when having values over the max`() {
        val repo = createTestRepo(
            stars = 2000000,
            forks = 200000,
            daysAgo = 0
        )
        val score = scorer.calculateScore(repo)
        assertEquals(1.0, score, "Repository with values higher than the max should get perfect score")
    }

    @Test
    fun `scoring is consistent`() {
        val repo = createTestRepo(stars = 500, forks = 200)
        val score1 = scorer.calculateScore(repo)
        val score2 = scorer.calculateScore(repo)
        assertEquals(score1, score2, "Scoring should be deterministic")
    }
}