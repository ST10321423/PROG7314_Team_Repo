package com.example.prog7314_universe.data

/**
 * Central configuration for the task API base URL. We attempt to read the value from the
 * generated BuildConfig when available, but fall back to a production endpoint so the
 * application can compile in environments where BuildConfig isn't generated (e.g. IDE-only).
 */
object TaskApiEnvironment {
    private const val DEFAULT_BASE_URL = "https://us-central1-prog7314-universe.cloudfunctions.net/api/"

    @Volatile
    private var overrideBaseUrl: String? = null

    /** Returns the currently configured base URL, normalized with a trailing slash. */
    fun baseUrl(): String = overrideBaseUrl ?: buildConfigBaseUrl() ?: DEFAULT_BASE_URL

    /** Allows tests or setup code to override the base URL at runtime. */
    fun setBaseUrl(baseUrl: String?) {
        overrideBaseUrl = baseUrl?.trim()?.takeIf { it.isNotEmpty() }?.let(::ensureTrailingSlash)
    }

    private fun buildConfigBaseUrl(): String? {
        val buildConfigClass = runCatching {
            Class.forName("com.example.prog7314_universe.BuildConfig")
        }.getOrNull()

        val value = buildConfigClass?.let {
            runCatching { it.getField("TASK_API_BASE_URL").get(null) as? String }.getOrNull()
        }

        return value?.trim()?.takeIf { it.isNotEmpty() }?.let(::ensureTrailingSlash)
    }

    private fun ensureTrailingSlash(url: String): String =
        if (url.endsWith('/')) url else "$url/"
}