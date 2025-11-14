package com.ingsis.grupo10.snippet.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer.withDefaults
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class OAuth2ResourceServerSecurityConfiguration(
    @Value("\${auth0.audience}") private val audience: String,
    @Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri}") private val issuer: String,
) {
    init {
        println("========================================")
        println("=== OAuth2 Configuration Loading ===")
        println("Issuer: $issuer")
        println("Audience: $audience")
        println("========================================")
    }

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        println("Configuring Security Filter Chain...")
        http
            .authorizeHttpRequests {
                it
                    .requestMatchers("/", "/health", "/actuator/health", "/public/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            }.oauth2ResourceServer { it.jwt(withDefaults()) }
            .cors(withDefaults())
            .csrf { it.disable() }

        return http.build()
    }

    @Bean
    fun jwtDecoder(): JwtDecoder {
        println("Creating JWT Decoder with issuer: $issuer")
        val jwtDecoder = NimbusJwtDecoder.withIssuerLocation(issuer).build()
        val audienceValidator: OAuth2TokenValidator<Jwt> = AudienceValidator(audience)
        val withIssuer: OAuth2TokenValidator<Jwt> = JwtValidators.createDefaultWithIssuer(issuer)
        val withAudience: OAuth2TokenValidator<Jwt> = DelegatingOAuth2TokenValidator(withIssuer, audienceValidator)
        jwtDecoder.setJwtValidator(withAudience)
        return jwtDecoder
    }
}
