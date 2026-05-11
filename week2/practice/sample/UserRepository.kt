// ⚠️ 이 파일은 멀티 에이전트 실습용 샘플입니다.
// 의도적으로 보안/품질/성능 문제를 포함하고 있습니다.

package com.example.app.data

import android.database.sqlite.SQLiteDatabase

class UserRepository(private val db: SQLiteDatabase) {

    // 🔴 보안: 하드코딩된 관리자 비밀번호
    val ADMIN_PASSWORD = "admin1234!"
    val SECRET_TOKEN = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"

    // 🔴 보안: SQL 인젝션 취약점 (사용자 입력을 직접 쿼리에 삽입)
    fun findUserByName(name: String): User? {
        val cursor = db.rawQuery(
            "SELECT * FROM users WHERE name = '$name'", null
        )
        return if (cursor.moveToFirst()) cursor.toUser() else null
    }

    // 🔴 성능: N+1 쿼리 문제
    // users 수만큼 orders 쿼리가 추가 실행됨
    fun getAllUsersWithOrders(): List<UserWithOrders> {
        val cursor = db.rawQuery("SELECT * FROM users", null)
        val result = mutableListOf<UserWithOrders>()

        while (cursor.moveToNext()) {
            val user = cursor.toUser()
            // 유저마다 별도 쿼리 실행 → 유저 100명이면 101번 쿼리
            val orderCursor = db.rawQuery(
                "SELECT * FROM orders WHERE user_id = ${user.id}", null
            )
            val orders = mutableListOf<Order>()
            while (orderCursor.moveToNext()) {
                orders.add(orderCursor.toOrder())
            }
            orderCursor.close()
            result.add(UserWithOrders(user, orders))
        }
        cursor.close()
        return result
    }

    // 🔴 품질: 함수 하나가 너무 많은 책임을 가짐 (SRP 위반)
    // 🔴 품질: 변수명이 의미를 전달하지 못함 (n, e, a, u)
    // 🔴 보안: SQL 인젝션 취약점
    fun processUser(id: Int, n: String, e: String, a: String): Boolean {
        val c = db.rawQuery("SELECT * FROM users WHERE id = $id", null)
        if (!c.moveToFirst()) return false
        val u = c.toUser()

        db.execSQL("UPDATE users SET name='$n', email='$e', address='$a' WHERE id=$id")
        sendVerificationEmail(e, "정보가 업데이트됐습니다")
        writeLog(id, "profile_update")
        clearCache(id)
        notifyAdminSlack(u.name, n)
        syncToRemoteServer(id)
        return true
    }

    // 🔴 성능: 캐싱 없이 매번 전체 계산
    // 🔴 성능: 빈 리스트일 때 0 나누기 → ArithmeticException
    fun getUserStats(userId: Int): UserStats {
        val cursor = db.rawQuery(
            "SELECT * FROM orders WHERE user_id = $userId", null
        )
        val orders = mutableListOf<Order>()
        while (cursor.moveToNext()) orders.add(cursor.toOrder())
        cursor.close()

        val totalAmount = orders.sumOf { it.amount }
        val orderCount = orders.size
        val avgAmount = totalAmount / orderCount  // ← orders가 비어있으면 예외 발생

        return UserStats(orderCount, totalAmount, avgAmount)
    }

    // 🔴 품질: Kotlin null safety 미활용, Java 스타일 null 체크
    fun getDisplayName(userId: Int): String {
        val cursor = db.rawQuery("SELECT name, nickname FROM users WHERE id = $userId", null)
        var name: String? = null
        var nickname: String? = null

        if (cursor.moveToFirst()) {
            name = cursor.getString(0)
            nickname = cursor.getString(1)
        }
        cursor.close()

        if (nickname != null && nickname.length > 0) {
            return nickname
        } else if (name != null && name.length > 0) {
            return name
        } else {
            return "Unknown"
        }
    }

    // stub 함수들
    private fun sendVerificationEmail(email: String, message: String) {}
    private fun writeLog(userId: Int, action: String) {}
    private fun clearCache(userId: Int) {}
    private fun notifyAdminSlack(oldName: String, newName: String) {}
    private fun syncToRemoteServer(userId: Int) {}
}

// 데이터 클래스
data class User(val id: Int, val name: String, val email: String)
data class Order(val id: Int, val userId: Int, val amount: Double)
data class UserWithOrders(val user: User, val orders: List<Order>)
data class UserStats(val orderCount: Int, val totalAmount: Double, val avgAmount: Double)

// 확장 함수 (cursor → 모델 변환)
fun android.database.Cursor.toUser() = User(
    id = getInt(getColumnIndexOrThrow("id")),
    name = getString(getColumnIndexOrThrow("name")),
    email = getString(getColumnIndexOrThrow("email"))
)
fun android.database.Cursor.toOrder() = Order(
    id = getInt(getColumnIndexOrThrow("id")),
    userId = getInt(getColumnIndexOrThrow("user_id")),
    amount = getDouble(getColumnIndexOrThrow("amount"))
)
