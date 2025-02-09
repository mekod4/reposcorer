package com.reposcorer.util

import com.reposcorer.model.GitHubRepo
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun createTestRepo(
    stars: Int = 0,
    forks: Int = 0,
    daysAgo: Long = 0
): GitHubRepo {
    val date = LocalDateTime.now().minusDays(daysAgo)
    return GitHubRepo(
        id = 1,
        name = "test-repo",
        fullName = "test/test-repo",
        stars = stars,
        forks = forks,
        updatedAt = date.format(DateTimeFormatter.ISO_DATE_TIME),
        createdAt = date.format(DateTimeFormatter.ISO_DATE_TIME),
        language = "kotlin"
    )
}