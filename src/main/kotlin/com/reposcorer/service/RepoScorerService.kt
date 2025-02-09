package com.reposcorer.service

import com.reposcorer.api.GitHubClient
import com.reposcorer.model.ScoredRepo
import com.reposcorer.scoring.RepoScorer
import org.slf4j.LoggerFactory
import java.time.LocalDate

class RepoScorerService(
    private val githubClient: GitHubClient,
    private val repoScorer: RepoScorer
) {
    private val logger = LoggerFactory.getLogger(RepoScorerService::class.java)

    suspend fun getTopRepositories(
        language: String,
        createdAfter: LocalDate = LocalDate.now().minusYears(1),
        limit: Int = 500
    ): Result<List<ScoredRepo>> {
        return githubClient.searchRepositories(language, createdAfter.toString())
            .map { response ->
                response.items
                    .map { repo ->
                        ScoredRepo(
                            repo = repo,
                            score = repoScorer.calculateScore(repo)
                        )
                    }
                    .sortedByDescending { it.score }
                    .take(limit)
                    .also {
                        logger.info("Successfully scored ${it.size} repositories")
                    }
            }
    }
}