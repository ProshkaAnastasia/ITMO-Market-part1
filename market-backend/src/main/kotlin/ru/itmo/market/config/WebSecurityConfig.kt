package ru.itmo.market.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import ru.itmo.market.security.UserDetailsServiceImpl
import ru.itmo.market.security.jwt.JwtAuthenticationFilter

@Configuration
@EnableWebSecurity
class WebSecurityConfig(
    @Autowired private val userDetailsService: UserDetailsServiceImpl,
    @Autowired private val passwordEncoder: PasswordEncoder,
    @Autowired private val jwtAuthenticationFilter: JwtAuthenticationFilter
) {

    @Bean
    fun authenticationProvider(): DaoAuthenticationProvider {
        return DaoAuthenticationProvider().apply {
            setUserDetailsService(userDetailsService)
            setPasswordEncoder(passwordEncoder)
        }
    }

    @Bean
    fun authenticationManager(http: HttpSecurity): AuthenticationManager {
        val auth = http.getSharedObject(AuthenticationManagerBuilder::class.java)
        auth.authenticationProvider(authenticationProvider())
        return auth.build()
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { authz ->
                authz
                    // Public endpoints
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                    .requestMatchers("/actuator/**").permitAll()
                    
                    // Product endpoints - read is public
                    .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/shops/**").permitAll()
                    
                    // Comments - read is public
                    .requestMatchers(HttpMethod.GET, "/api/products/*/comments/**").permitAll()
                    
                    // Protected endpoints
                    .requestMatchers("/api/cart/**").authenticated()
                    .requestMatchers("/api/orders/**").authenticated()
                    .requestMatchers("/api/users/**").authenticated()
                    
                    // Admin/Moderation endpoints
                    .requestMatchers("/api/moderation/**").hasAnyRole("MODERATOR", "ADMIN")
                    
                    // Default deny
                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .exceptionHandling { ex ->
                ex.authenticationEntryPoint { request, response, authException ->
                    response.status = 401
                    response.contentType = "application/json;charset=UTF-8"
                    response.writer.write("""{"error":"Unauthorized","message":"${authException.message}"}""")
                }
            }

        return http.build()
    }
}