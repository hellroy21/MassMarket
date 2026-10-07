package ru.massmarket.server

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object Users : LongIdTable("users") {
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 100)
    val name = varchar("name", 100)
    val birthDate = varchar("birth_date", 10).nullable()
    val avatarBase64 = text("avatar_base64").nullable()
}

object Products : LongIdTable("products") {
    val name = varchar("name", 120)
    val category = varchar("category", 80)
    val description = varchar("description", 500)
    val price = integer("price")
    val unit = varchar("unit", 30)
    val emoji = varchar("emoji", 10)
}

object Orders : LongIdTable("orders") {
    val userId = reference("user_id", Users)
    val address = varchar("address", 500)
    val productsTotal = integer("products_total")
    val deliveryPrice = integer("delivery_price")
    val total = integer("total")
    val createdAt = varchar("created_at", 40)
}

object OrderItems : LongIdTable("order_items") {
    val orderId = reference("order_id", Orders)
    val productId = long("product_id")
    val productName = varchar("product_name", 120)
    val price = integer("price")
    val quantity = integer("quantity")
    val emoji = varchar("emoji", 10)
}

object DatabaseFactory {
    fun init() {
        val jdbcUrl = System.getProperty("massmarket.jdbcUrl")
            ?: System.getenv("JDBC_URL")
            ?: "jdbc:postgresql://localhost:5432/massmarket"
        val config = HikariConfig().apply {
            driverClassName = if (jdbcUrl.startsWith("jdbc:h2:")) "org.h2.Driver" else "org.postgresql.Driver"
            this.jdbcUrl = jdbcUrl
            username = System.getProperty("massmarket.dbUser") ?: System.getenv("DB_USER") ?: "massmarket"
            password = System.getProperty("massmarket.dbPassword") ?: System.getenv("DB_PASSWORD") ?: "massmarket"
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        Database.connect(HikariDataSource(config))
        transaction {
            SchemaUtils.create(Users, Products, Orders, OrderItems)
            seedProducts()
        }
    }

    private fun seedProducts() {
        if (Products.selectAll().count() > 0) return
        val products = listOf(
            ProductSeed("Молоко 3,2%", "Молочные продукты", "Свежее пастеризованное молоко, 1 л", 99, "1 л", "🥛"),
            ProductSeed("Сыр Российский", "Молочные продукты", "Полутвёрдый сыр, 200 г", 249, "200 г", "🧀"),
            ProductSeed("Йогурт клубничный", "Молочные продукты", "Натуральный йогурт, 150 г", 79, "150 г", "🥣"),
            ProductSeed("Филе куриное", "Мясо", "Охлаждённое куриное филе", 389, "1 кг", "🍗"),
            ProductSeed("Фарш домашний", "Мясо", "Свинина и говядина", 459, "1 кг", "🥩"),
            ProductSeed("Яблоки", "Овощи и фрукты", "Сладкие сезонные яблоки", 139, "1 кг", "🍎"),
            ProductSeed("Бананы", "Овощи и фрукты", "Спелые бананы", 129, "1 кг", "🍌"),
            ProductSeed("Помидоры", "Овощи и фрукты", "Розовые помидоры", 229, "1 кг", "🍅"),
            ProductSeed("Хлеб Бородинский", "Хлеб и выпечка", "Ржаной заварной хлеб", 69, "400 г", "🍞"),
            ProductSeed("Круассан", "Хлеб и выпечка", "Сливочный круассан", 89, "1 шт", "🥐"),
            ProductSeed("Сок апельсиновый", "Напитки", "Сок без добавления сахара", 159, "1 л", "🧃"),
            ProductSeed("Вода минеральная", "Напитки", "Негазированная вода", 59, "1.5 л", "💧")
        )
        products.forEach { item ->
            Products.insert {
                it[name] = item.name
                it[category] = item.category
                it[description] = item.description
                it[price] = item.price
                it[unit] = item.unit
                it[emoji] = item.emoji
            }
        }
    }

    private data class ProductSeed(
        val name: String,
        val category: String,
        val description: String,
        val price: Int,
        val unit: String,
        val emoji: String
    )
}
