package ru.massmarket.server

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.mindrot.jbcrypt.BCrypt
import java.time.Instant
import java.util.Date

private const val DELIVERY_PRICE = 150
private const val ISSUER = "massmarket-server"
private const val AUDIENCE = "massmarket-client"

fun Application.module() {
    DatabaseFactory.init()
    val secret = System.getenv("JWT_SECRET") ?: "local-development-secret"
    val appLog = environment.log

    install(CallLogging)
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; prettyPrint = true })
    }
    install(StatusPages) {
        exception<ApiException> { call, cause ->
            call.respond(cause.status, MessageResponse(cause.message))
        }
        exception<Throwable> { call, cause ->
            appLog.error("Unhandled error", cause)
            call.respond(HttpStatusCode.InternalServerError, MessageResponse("Внутренняя ошибка сервера"))
        }
    }
    install(Authentication) {
        jwt("auth-jwt") {
            realm = "MassMarket"
            verifier(JWT.require(Algorithm.HMAC256(secret)).withAudience(AUDIENCE).withIssuer(ISSUER).build())
            validate { credential ->
                credential.payload.getClaim("userId").asLong()?.let { JWTPrincipal(credential.payload) }
            }
        }
    }

    routing {
        get("/health") { call.respond(MessageResponse("OK")) }

        route("/api/auth") {
            post("/register") {
                val request = call.receive<RegisterRequest>()
                val email = request.email.trim().lowercase()
                if (!email.matches(Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))) {
                    throw ApiException(HttpStatusCode.BadRequest, "Введите корректную почту")
                }
                if (request.password.length < 6) throw ApiException(HttpStatusCode.BadRequest, "Пароль должен содержать минимум 6 символов")
                if (request.password != request.passwordConfirmation) throw ApiException(HttpStatusCode.BadRequest, "Пароли не совпадают")

                val userId = transaction {
                    if (Users.selectAll().where { Users.email eq email }.count() > 0) {
                        throw ApiException(HttpStatusCode.Conflict, "Пользователь с такой почтой уже существует")
                    }
                    Users.insert {
                        it[Users.email] = email
                        it[passwordHash] = BCrypt.hashpw(request.password, BCrypt.gensalt())
                        it[name] = request.name.trim().ifBlank { "Покупатель" }
                    } get Users.id
                }.value
                val user = loadUser(userId)
                call.respond(HttpStatusCode.Created, AuthResponse(createToken(userId, secret), user))
            }

            post("/login") {
                val request = call.receive<AuthRequest>()
                val row = transaction {
                    Users.selectAll().where { Users.email eq request.email.trim().lowercase() }.singleOrNull()
                } ?: throw ApiException(HttpStatusCode.Unauthorized, "Неверная почта или пароль")
                if (!BCrypt.checkpw(request.password, row[Users.passwordHash])) {
                    throw ApiException(HttpStatusCode.Unauthorized, "Неверная почта или пароль")
                }
                val userId = row[Users.id].value
                call.respond(AuthResponse(createToken(userId, secret), loadUser(userId)))
            }
        }

        get("/api/products") {
            val products = transaction {
                Products.selectAll().orderBy(Products.category to org.jetbrains.exposed.sql.SortOrder.ASC).map { row ->
                    ProductResponse(row[Products.id].value, row[Products.name], row[Products.category], row[Products.description], row[Products.price], row[Products.unit], row[Products.emoji])
                }
            }
            call.respond(products)
        }

        authenticate("auth-jwt") {
            route("/api/profile") {
                get { call.respond(loadUser(call.userId())) }
                put {
                    val userId = call.userId()
                    val request = call.receive<UpdateProfileRequest>()
                    if (request.name.isBlank()) throw ApiException(HttpStatusCode.BadRequest, "Имя не может быть пустым")
                    if (request.avatarBase64 != null && request.avatarBase64.length > 2_800_000) {
                        throw ApiException(HttpStatusCode.BadRequest, "Изображение слишком большое")
                    }
                    transaction {
                        Users.update({ Users.id eq userId }) {
                            it[name] = request.name.trim()
                            it[birthDate] = request.birthDate?.trim()?.ifBlank { null }
                            it[avatarBase64] = request.avatarBase64
                        }
                    }
                    call.respond(loadUser(userId))
                }
            }

            route("/api/orders") {
                get { call.respond(loadOrders(call.userId())) }
                post {
                    val userId = call.userId()
                    val request = call.receive<CreateOrderRequest>()
                    if (request.address.isBlank()) throw ApiException(HttpStatusCode.BadRequest, "Укажите адрес доставки")
                    if (request.items.isEmpty()) throw ApiException(HttpStatusCode.BadRequest, "Корзина пуста")
                    if (request.items.any { it.quantity !in 1..99 }) throw ApiException(HttpStatusCode.BadRequest, "Некорректное количество товара")

                    val orderId = transaction {
                        val ids = request.items.map { it.productId }.toSet()
                        val productRows = Products.selectAll().where { Products.id inList ids }.associateBy { it[Products.id].value }
                        if (productRows.size != ids.size) throw ApiException(HttpStatusCode.BadRequest, "Один из товаров не найден")
                        val productsTotal = request.items.sumOf { item -> productRows.getValue(item.productId)[Products.price] * item.quantity }
                        val id = Orders.insert {
                            it[Orders.userId] = userId
                            it[address] = request.address.trim()
                            it[Orders.productsTotal] = productsTotal
                            it[deliveryPrice] = DELIVERY_PRICE
                            it[total] = productsTotal + DELIVERY_PRICE
                            it[createdAt] = Instant.now().toString()
                        } get Orders.id
                        request.items.forEach { item ->
                            val product = productRows.getValue(item.productId)
                            OrderItems.insert {
                                it[orderId] = id
                                it[productId] = item.productId
                                it[productName] = product[Products.name]
                                it[price] = product[Products.price]
                                it[quantity] = item.quantity
                                it[emoji] = product[Products.emoji]
                            }
                        }
                        id.value
                    }
                    call.respond(HttpStatusCode.Created, loadOrders(userId).first { it.id == orderId })
                }
            }
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.userId(): Long =
    principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()
        ?: throw ApiException(HttpStatusCode.Unauthorized, "Требуется авторизация")

private fun createToken(userId: Long, secret: String): String = JWT.create()
    .withAudience(AUDIENCE)
    .withIssuer(ISSUER)
    .withClaim("userId", userId)
    .withExpiresAt(Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000))
    .sign(Algorithm.HMAC256(secret))

private fun loadUser(userId: Long): UserResponse = transaction {
    val row = Users.selectAll().where { Users.id eq userId }.singleOrNull()
        ?: throw ApiException(HttpStatusCode.NotFound, "Пользователь не найден")
    UserResponse(row[Users.id].value, row[Users.email], row[Users.name], row[Users.birthDate], row[Users.avatarBase64])
}

private fun loadOrders(userId: Long): List<OrderResponse> = transaction {
    Orders.selectAll().where { Orders.userId eq userId }
        .orderBy(Orders.id to org.jetbrains.exposed.sql.SortOrder.DESC)
        .map { order ->
            val id = order[Orders.id].value
            val items = OrderItems.selectAll().where { OrderItems.orderId eq id }.map { item ->
                OrderItemResponse(item[OrderItems.productId], item[OrderItems.productName], item[OrderItems.price], item[OrderItems.quantity], item[OrderItems.emoji])
            }
            OrderResponse(id, order[Orders.address], order[Orders.productsTotal], order[Orders.deliveryPrice], order[Orders.total], order[Orders.createdAt], items)
        }
}
