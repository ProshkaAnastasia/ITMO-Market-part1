package ru.itmo.market.security.jwt

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    @Autowired private val jwtTokenProvider: JwtTokenProvider
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val token = extractToken(request)
            if (token != null && jwtTokenProvider.isTokenValid(token)) {
                val userId = jwtTokenProvider.getUserIdFromToken(token)
                val username = jwtTokenProvider.getUsernameFromToken(token)
                val roles = jwtTokenProvider.getRolesFromToken(token)

                val authorities = roles.map { role ->
                    SimpleGrantedAuthority("ROLE_$role")
                }

                val authentication = UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    authorities
                )
                authentication.details = mapOf(
                    "userId" to userId,
                    "username" to username,
                    "roles" to roles
                )
                SecurityContextHolder.getContext().authentication = authentication
            }
        } catch (e: Exception) {
            logger.error("Cannot set user authentication in security context", e)
        }

        filterChain.doFilter(request, response)
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        if (bearerToken?.startsWith("Bearer ") == true) {
            return bearerToken.substring(7)
        }
        return null
    }
}