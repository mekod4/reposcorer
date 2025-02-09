package com.reposcorer.api

import com.reposcorer.config.AppConfig
import com.reposcorer.model.GitHubRepo
import com.reposcorer.model.SearchResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import org.slf4j.LoggerFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class GitHubClient(config: AppConfig) {
    private val logger = LoggerFactory.getLogger(GitHubClient::class.java)
    private val service: GitHubService

    init {
        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        // Add authentication if token is present. See README for details.
        config.githubToken?.let { token ->
            clientBuilder.addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
                chain.proceed(request)
            }
        }

        val client = clientBuilder.build()

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(config.baseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        service = retrofit.create(GitHubService::class.java)
    }

    suspend fun searchRepositories(
        language: String,
        createdDate: String,
        maxRetries: Int = 3,
        initialDelayMs: Long = 1000
    ): Result<SearchResponse> {
        return try {
            val allItems = mutableListOf<GitHubRepo>()
            var page = 1
            var hasNextPage = true
            var totalCount = 0

            while (hasNextPage) {
                val pageResult = fetchPage(language, createdDate, page, maxRetries, initialDelayMs)
                when {
                    pageResult.isSuccess -> {
                        val searchResponse = pageResult.getOrNull()!!
                        totalCount = searchResponse.totalCount
                        allItems.addAll(searchResponse.items)

                        // Check if there are more pages based on the items received
                        hasNextPage = allItems.size < totalCount && searchResponse.items.isNotEmpty()
                        page++

                        logger.info("Fetched page $page, total items so far: ${allItems.size} out of $totalCount")
                    }

                    pageResult.isFailure -> {
                        return Result.failure(pageResult.exceptionOrNull()!!)
                    }
                }
            }

            Result.success(
                SearchResponse(
                    totalCount = totalCount,
                    incompleteResults = false,
                    items = allItems
                )
            )
        } catch (e: Exception) {
            logger.error("Error fetching all repository pages", e)
            Result.failure(e)
        }
    }

    private suspend fun fetchPage(
        language: String,
        createdDate: String,
        page: Int,
        maxRetries: Int,
        initialDelayMs: Long
    ): Result<SearchResponse> {
        var currentDelay = initialDelayMs
        repeat(maxRetries) { attempt ->
            try {
                val response = service.searchRepositories("language:$language created:>$createdDate", page = page)

                if (response.isSuccessful) {
                    val body = response.body()

                    if (body != null) {
                        return Result.success(body)
                    }

                    logger.error("Response successful but body was null")
                    return Result.failure(IOException("Empty response body"))
                }

                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                logger.error("API request failed with code ${response.code()}: $errorBody")

                if (response.code() == 403) {
                    logger.warn("Rate limit exceeded, waiting longer before retry")
                    currentDelay *= 2
                }

            } catch (e: SocketTimeoutException) {
                logger.error("Network timeout on attempt ${attempt + 1}", e)
            } catch (e: IOException) {
                logger.error("Network error on attempt ${attempt + 1}", e)
            } catch (e: Exception) {
                logger.error("Unexpected error on attempt ${attempt + 1}", e)
                return Result.failure(e)
            }

            if (attempt < maxRetries - 1) {
                delay(currentDelay)
                currentDelay *= 2
            }
        }

        return Result.failure(IOException("Failed to fetch page $page after $maxRetries attempts"))
    }
}