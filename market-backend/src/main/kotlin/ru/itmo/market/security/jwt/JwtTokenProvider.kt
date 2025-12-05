package ru.itmo.market.security.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret:your-super-secret-key-change-in-production-min-256-bits-example-here-1234567890}")
    private val jwtSecret: String,

    @Value("\${jwt.access-token-expiration:900000}")
    private val accessTokenExpiration: Long,

    @Value("\${jwt.refresh-token-expiration:604800000}")
    private val refreshTokenExpiration: Long
) {

    private fun getSigningKey(): SecretKey {
        return Keys.hmacShaKeyFor(jwtSecret.toByteArray())
    }

    fun generateAccessToken(userId: Long, username: String, roles: Set<String>): String {
        return Jwts.builder()
            .setSubject(userId.toString())
            .claim("username", username)
            .claim("roles", roles)
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + accessTokenExpiration))
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact()
    }

    fun generateRefreshToken(userId: Long): String {
        return Jwts.builder()
            .setSubject(userId.toString())
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + refreshTokenExpiration))
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact()
    }

    fun getUserIdFromToken(token: String): Long {
        return parseClaims(token).subject.toLong()
    }

    fun getUsernameFromToken(token: String): String {
        return parseClaims(token).get("username", String::class.java)
    }

    @Suppress("UNCHECKED_CAST")
    fun getRolesFromToken(token: String): Set<String> {
        return parseClaims(token).get("roles", List::class.java).toSet() as Set<String>
    }

    fun isTokenValid(token: String): Boolean {
        return try {
            parseClaims(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun parseClaims(token: String): Claims {
        return Jwts.parser()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .body
    }
}