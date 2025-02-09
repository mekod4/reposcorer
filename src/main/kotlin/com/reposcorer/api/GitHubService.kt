package com.reposcorer.api

import com.reposcorer.model.SearchResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface GitHubService {
    @Headers(
        "Accept: application/vnd.github.v3+json",
        "X-GitHub-Api-Version: 2022-11-28"
    )
    @GET("search/repositories")
    suspend fun searchRepositories(
        @Query("q") query: String,
        @Query("sort") sort: String = "stars",
        @Query("order") order: String = "desc",
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1
    ): Response<SearchResponse>
}