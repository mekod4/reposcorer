package com.reposcorer.config

open class AppConfig(
    val githubToken: String? = null,
    open val baseUrl: String = "https://api.github.com"
)