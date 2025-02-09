package com.reposcorer.model

data class ScoredRepo(
    val repo: GitHubRepo,
    val score: Double
)