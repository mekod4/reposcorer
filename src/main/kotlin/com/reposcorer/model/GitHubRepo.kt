package com.reposcorer.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GitHubRepo(
    val id: Long,
    val name: String,
    @Json(name = "full_name")
    val fullName: String,
    @Json(name = "stargazers_count")
    val stars: Int,
    @Json(name = "forks_count")
    val forks: Int,
    @Json(name = "updated_at")
    val updatedAt: String,
    @Json(name = "created_at")
    val createdAt: String,
    val language: String?
)