package ru.massmarket.server

import kotlinx.serialization.Serializable

@Serializable
data class MessageResponse(val message: String)

@Serializable
data class AuthRequest(val email: String, val password: String)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val passwordConfirmation: String,
    val name: String = "Покупатель"
)

@Serializable
data class AuthResponse(val token: String, val user: UserResponse)

@Serializable
data class UserResponse(
    val id: Long,
    val email: String,
    val name: String,
    val birthDate: String? = null,
    val avatarBase64: String? = null
)

@Serializable
data class UpdateProfileRequest(
    val name: String,
    val birthDate: String? = null,
    val avatarBase64: String? = null
)

@Serializable
data class ProductResponse(
    val id: Long,
    val name: String,
    val category: String,
    val description: String,
    val price: Int,
    val unit: String,
    val emoji: String
)

@Serializable
data class OrderItemRequest(val productId: Long, val quantity: Int)

@Serializable
data class CreateOrderRequest(val address: String, val items: List<OrderItemRequest>)

@Serializable
data class OrderItemResponse(
    val productId: Long,
    val name: String,
    val price: Int,
    val quantity: Int,
    val emoji: String
)

@Serializable
data class OrderResponse(
    val id: Long,
    val address: String,
    val productsTotal: Int,
    val deliveryPrice: Int,
    val total: Int,
    val createdAt: String,
    val items: List<OrderItemResponse>
)

class ApiException(val status: io.ktor.http.HttpStatusCode, override val message: String) : RuntimeException(message)

