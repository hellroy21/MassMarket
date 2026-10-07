package ru.massmarket.server

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OrderIsolationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `orders belong only to authenticated user`() = testApplication {
        System.setProperty("massmarket.jdbcUrl", "jdbc:h2:mem:orders;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
        System.setProperty("massmarket.dbUser", "sa")
        System.setProperty("massmarket.dbPassword", "")
        application { module() }

        val products = json.decodeFromString<List<ProductResponse>>(client.get("/api/products").bodyAsText())
        val milk = products.first { it.name.startsWith("Молоко") }
        val bread = products.first { it.name.startsWith("Хлеб") }
        val first = register("first@example.test")
        val second = register("second@example.test")

        createOrder(first.token, milk.id)
        createOrder(second.token, bread.id)

        val firstOrders = orders(first.token)
        val secondOrders = orders(second.token)
        assertEquals(listOf("Молоко 3,2%"), firstOrders.single().items.map { it.name })
        assertEquals(listOf("Хлеб Бородинский"), secondOrders.single().items.map { it.name })
        assertTrue(firstOrders.single().total == milk.price + 150)
    }

    private suspend fun io.ktor.server.testing.ApplicationTestBuilder.register(email: String): AuthResponse {
        val response = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(RegisterRequest(email, "secret12", "secret12")))
        }
        return json.decodeFromString(response.bodyAsText())
    }

    private suspend fun io.ktor.server.testing.ApplicationTestBuilder.createOrder(token: String, productId: Long) {
        client.post("/api/orders") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(CreateOrderRequest("Тестовый адрес", listOf(OrderItemRequest(productId, 1)))))
        }
    }

    private suspend fun io.ktor.server.testing.ApplicationTestBuilder.orders(token: String): List<OrderResponse> {
        val response = client.get("/api/orders") { bearerAuth(token) }
        return json.decodeFromString(response.bodyAsText())
    }
}
