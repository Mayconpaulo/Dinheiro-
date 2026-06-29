package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Path
import java.util.concurrent.TimeUnit
import com.example.BuildConfig

// --- Gemini Request / Response Data Classes ---

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "responseMimeType") val responseMimeType: String? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>,
    @Json(name = "role") val role: String? = null
)

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content?
)

// --- Retrofit API Service ---

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

// --- Moshi and Retrofit Setup ---

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }
}

// --- API Client ---

object GeminiApiClient {
    suspend fun generateResponse(prompt: String, systemPrompt: String = "", history: List<Content> = emptyList()): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "Erro: Chave API do Gemini não configurada. Por favor, configure a chave GEMINI_API_KEY no painel de segredos do AI Studio."
        }

        // Build list of contents, mixing history with the current prompt
        val contents = mutableListOf<Content>()
        contents.addAll(history)
        contents.add(Content(parts = listOf(Part(text = prompt)), role = "user"))

        val systemInstruction = if (systemPrompt.isNotEmpty()) {
            Content(parts = listOf(Part(text = systemPrompt)))
        } else {
            null
        }

        // Let's configure the generation config.
        val requestWithJson = GenerateContentRequest(
            contents = contents,
            systemInstruction = systemInstruction,
            generationConfig = GenerationConfig(responseMimeType = "application/json")
        )

        val requestWithoutJson = GenerateContentRequest(
            contents = contents,
            systemInstruction = systemInstruction,
            generationConfig = null
        )

        // Try standard fallback models. We will iterate over combinations of modern supported models
        // and check if they succeed, starting with gemini-3.5-flash which is recommended for basic text tasks.
        val modelsToTry = listOf(
            "gemini-3.5-flash",
            "gemini-3.1-flash-lite-preview",
            "gemini-3.1-pro-preview",
            "gemini-2.5-flash"
        )

        var lastException: Exception? = null

        // 1st stage: Try each model with JSON enforcement
        for (modelName in modelsToTry) {
            try {
                val response = RetrofitClient.service.generateContent(modelName, apiKey, requestWithJson)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrBlank()) {
                    return text
                }
            } catch (e: Exception) {
                lastException = e
            }
        }

        // 2nd stage fallback: Try each model without JSON enforcement (relying purely on prompt instruction)
        for (modelName in modelsToTry) {
            try {
                val response = RetrofitClient.service.generateContent(modelName, apiKey, requestWithoutJson)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrBlank()) {
                    return text
                }
            } catch (e: Exception) {
                lastException = e
            }
        }

        val e = lastException ?: Exception("Ocorreu um problema desconhecido ao falar com o Gemini.")
        
        return if (e is retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string() ?: ""
            if (errorBody.contains("leaked", ignoreCase = true) || errorBody.contains("PERMISSION_DENIED", ignoreCase = true)) {
                "⚠️ Sua chave de API do Gemini (GEMINI_API_KEY) foi bloqueada/desativada pela Google por segurança (chave reportada como exposta/vazada).\n\n" +
                "Para corrigir isso e fazer o Chat funcionar imediatamente:\n\n" +
                "1️⃣ Vá ao **Google AI Studio** e crie uma nova chave de API.\n" +
                "2️⃣ No painel lateral esquerdo do AI Studio, clique em **Secrets** (ícone de chave 🔑).\n" +
                "3️⃣ Edite ou adicione a variável **GEMINI_API_KEY** colando a nova chave gerada.\n" +
                "4️⃣ Volte ao chat e tente sua mensagem novamente!"
            } else {
                "Erro ao falar com o assistente inteligente: HTTP ${e.code()} (Detalhe técnico: HttpException - HTTP ${e.code()} - $errorBody)"
            }
        } else {
            "Erro ao falar com o assistente inteligente: ${e.localizedMessage ?: "Ocorreu um problema de conexão."} (Detalhe técnico: ${e.javaClass.simpleName} - ${e.message})"
        }
    }
}
