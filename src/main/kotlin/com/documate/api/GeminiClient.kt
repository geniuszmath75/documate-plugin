package com.documate.api

import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

object GeminiClient {
    private const val MODEL = "gemini-2.5-flash"
    private val API_URL get() = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"
    private const val MAX_TOKENS = 1024
    private const val TIMEOUT_SECONDS = 30L

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
        .build()

    private val gson = Gson()

    fun generateDocumentation(apiKey: String, sourceCode: String): Result<String> {
        // Truncate overly large inputs to avoid huge API bills and timeouts
        val truncatedCode = sourceCode.take(4000)

        val prompt = buildPrompt(truncatedCode)
        val requestBody = buildRequestBody(prompt)

        val request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL))
            .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
            .header("Content-Type", "application/json")
            .header("x-goog-api-key", apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()

        return try {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() == 401) {
                return Result.failure(IOException("Invalid API key. Please check your key in Settings -> Tools -> DocuMate."))
            }

            if (response.statusCode() != 200) {
                // Avoid leaking raw server response which may contain sensitive details
                return Result.failure(IOException("API request failed with status ${response.statusCode()}."))
            }

            val docComment = parseResponse(response.body())
            Result.success(docComment)
        } catch (e: IOException) {
            Result.failure(IOException("Network error: ${e.message}"))
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            Result.failure(IOException("Request was interrupted."))
        }
    }

    private fun buildPrompt(sourceCode: String): String = """
        You are a documentation generator for Kotlin and Java code.
        
        Generate a KDoc comment (for Kotlin) or Javadoc comment (for Java) for the following code element.
        
        Rules:
        - Output ONLY the doc comment block, starting with /** and ending with */
        - Do NOT include the original code
        - Do NOT include any explanation or markdown formatting
        - Include @param tags for each parameter
        - Include @return tag if the function returns a non-Unit/void value
        - Include @throws tag if exceptions are thrown
        - Keep the description concise and accurate
        
        Code:
        ```
        $sourceCode
        ```
    """.trimIndent()

    private fun buildRequestBody(prompt: String): String {
        val body = JsonObject().apply {
            add(
                "contents", gson.toJsonTree(
                    listOf(
                        mapOf(
                            "parts" to listOf(
                                mapOf("text" to prompt)
                            )
                        )
                    )
                )
            )
            add(
                "generationConfig", gson.toJsonTree(
                    mapOf("maxOutputTokens" to MAX_TOKENS)
                )
            )
        }
        return gson.toJson(body)
    }

    private fun parseResponse(responseBody: String): String {
        val json = gson.fromJson(responseBody, JsonObject::class.java)
        val text = json
            .getAsJsonArray("candidates")
            ?.get(0)?.asJsonObject
            ?.getAsJsonObject("content")
            ?.getAsJsonArray("parts")
            ?.get(0)?.asJsonObject
            ?.get("text")?.asString
            ?: throw IOException("Unexpected API response format.")

        return text.trim()
    }
}