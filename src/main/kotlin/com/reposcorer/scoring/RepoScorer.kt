package com.reposcorer.scoring

import com.reposcorer.model.GitHubRepo
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Simple abstraction, based on MinMax Scaling
class RepoScorer(
    private val starsWeight: Double = 0.5,
    private val forksWeight: Double = 0.3,
    private val recencyWeight: Double = 0.2
) {
    init {
        require(starsWeight + forksWeight + recencyWeight == 1.0) {
            "Weights must sum to 1.0"
        }
    }

    fun calculateScore(repo: GitHubRepo): Double {
        val formatter = DateTimeFormatter.ISO_DATE_TIME
        val updatedAt = LocalDateTime.parse(repo.updatedAt, formatter)
        val now = LocalDateTime.now()

        // Days since the repo's last update
        val daysSinceUpdate = Duration.between(updatedAt, now).toDays()

        // Normalize repo's values
        val normalizedStars = normalize(repo.stars.toDouble(), 0.0, 1000000.0)
        val normalizedForks = normalize(repo.forks.toDouble(), 0.0, 100000.0)
        val normalizedRecency = normalize(365.0 - daysSinceUpdate, 0.0, 365.0)

        val score = (normalizedStars * starsWeight + normalizedForks * forksWeight + normalizedRecency * recencyWeight)

        return score
    }

    private fun normalize(value: Double, min: Double, max: Double): Double {
        return when {
            value >= max -> 1.0
            value <= min -> 0.0
            else -> (value - min) / (max - min)
        }
    }
}