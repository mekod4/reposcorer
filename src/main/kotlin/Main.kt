package com.reposcorer

import com.reposcorer.api.GitHubClient
import com.reposcorer.config.AppConfig
import com.reposcorer.scoring.RepoScorer
import com.reposcorer.service.RepoScorerService
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.format.DateTimeParseException

fun main() {
    val config = AppConfig()
    val githubClient = GitHubClient(config)
    val scorer = RepoScorer()
    val service = RepoScorerService(githubClient, scorer)

    println("Enter the programming language to search for (default: kotlin):")
    val language = readlnOrNull()?.takeIf { it.isNotBlank() } ?: "kotlin"

    println("Enter the earliest creation date (with format YYYY-MM-DD, default: 1 year ago):")
    val createdDate = readlnOrNull()?.let { input ->
        try {
            LocalDate.parse(input)
        } catch (e: DateTimeParseException) {
            LocalDate.now().minusYears(1)
        }
    } ?: LocalDate.now().minusYears(1)

    runBlocking {
        service.getTopRepositories(language, createdDate)
            .onSuccess { repos ->
                if (repos.isEmpty()) {
                    println("\nNo repositories found matching the criteria.")
                } else {
                    println("\nFound top repositories:")
                    repos.forEach { scoredRepo ->
                        println("""
                            |Repository: ${scoredRepo.repo.fullName}
                            |Stars: ${scoredRepo.repo.stars}
                            |Forks: ${scoredRepo.repo.forks}
                            |Updated: ${scoredRepo.repo.updatedAt}
                            |Score: ${String.format("%.2f", scoredRepo.score)}
                            |${"-".repeat(50)}
                        """.trimMargin())
                    }
                }
            }
            .onFailure { error ->
                println("Error: ${error.message}")
            }
    }
}