package ru.massmarket.app

import kotlinx.serialization.Serializable

@Serializable data class MessageResponse(val message: String)
@Serializable data class AuthRequest(val email: String, val password: String)
@Serializable data class RegisterRequest(val email: String, val password: String, val passwordConfirmation: String, val name: String = "Покупатель")
@Serializable data class AuthResponse(val token: String, val user: User)
@Serializable data class User(val id: Long, val email: String, val name: String, val birthDate: String? = null, val avatarBase64: String? = null)
@Serializable data class UpdateProfileRequest(val name: String, val birthDate: String? = null, val avatarBase64: String? = null)
@Serializable data class Product(val id: Long, val name: String, val category: String, val description: String, val price: Int, val unit: String, val emoji: String)
@Serializable data class OrderItemRequest(val productId: Long, val quantity: Int)
@Serializable data class CreateOrderRequest(val address: String, val items: List<OrderItemRequest>)
@Serializable data class OrderItem(val productId: Long, val name: String, val price: Int, val quantity: Int, val emoji: String)
@Serializable data class Order(val id: Long, val address: String, val productsTotal: Int, val deliveryPrice: Int, val total: Int, val createdAt: String, val items: List<OrderItem>)

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class MainSection { CATALOG, CART, ORDERS, PROFILE }

