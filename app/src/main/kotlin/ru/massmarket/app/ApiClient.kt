package ru.massmarket.app

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class ApiClient(serverUrl: String) {
    private val baseUrl = serverUrl.trim().trimEnd('/')
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; explicitNulls = false })
        }
    }

    suspend fun login(email: String, password: String): AuthResponse =
        request { client.post("$baseUrl/api/auth/login") { contentType(ContentType.Application.Json); setBody(AuthRequest(email, password)) } }

    suspend fun register(email: String, password: String, confirmation: String): AuthResponse =
        request { client.post("$baseUrl/api/auth/register") { contentType(ContentType.Application.Json); setBody(RegisterRequest(email, password, confirmation)) } }

    suspend fun products(): List<Product> = request { client.get("$baseUrl/api/products") }

    suspend fun profile(token: String): User = request { client.get("$baseUrl/api/profile") { bearerAuth(token) } }

    suspend fun updateProfile(token: String, body: UpdateProfileRequest): User = request {
        client.put("$baseUrl/api/profile") { bearerAuth(token); contentType(ContentType.Application.Json); setBody(body) }
    }

    suspend fun orders(token: String): List<Order> = request { client.get("$baseUrl/api/orders") { bearerAuth(token) } }

    suspend fun createOrder(token: String, body: CreateOrderRequest): Order = request {
        client.post("$baseUrl/api/orders") { bearerAuth(token); contentType(ContentType.Application.Json); setBody(body) }
    }

    private suspend inline fun <reified T> request(block: suspend () -> io.ktor.client.statement.HttpResponse): T {
        try {
            val response = block()
            if (response.status.value !in 200..299) {
                val message = runCatching { response.body<MessageResponse>().message }.getOrNull()
                    ?: "Ошибка сервера: ${response.status.value}"
                throw ApiError(message)
            }
            return response.body()
        } catch (error: ApiError) {
            throw error
        } catch (error: Exception) {
            throw ApiError("Не удалось подключиться к серверу. Проверьте адрес и сеть.")
        }
    }
}

class ApiError(override val message: String) : Exception(message)
