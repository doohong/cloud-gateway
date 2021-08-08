package com.cloud.gateway.filter

import io.jsonwebtoken.Jwts
import org.apache.http.HttpHeaders
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class AuthorizationHeaderFilter(
        private val env: Environment
) : AbstractGatewayFilterFactory<AuthorizationHeaderFilter.Config>() {

    object Config {
    }

    override fun apply(config: Config): GatewayFilter {
        return GatewayFilter { exchange, chain ->
            var request = exchange.request

            if (!request.headers.containsKey(HttpHeaders.AUTHORIZATION)) {
                return@GatewayFilter onError(exchange, "no authorization header", HttpStatus.UNAUTHORIZED)
            }
            val authorizationHeader = request.headers[HttpHeaders.AUTHORIZATION]!![0]
            val jwt = authorizationHeader.replace("Bearer", "")

            if (!isJwtValid(jwt)) {
                return@GatewayFilter onError(exchange, "JWT token is not valid", HttpStatus.UNAUTHORIZED)
            }

            return@GatewayFilter chain.filter(exchange)
        }
    }

    private fun isJwtValid(jwt: String): Boolean {
        var returnValue = true;
        var subject = ""
        try {
            subject = Jwts.parser().setSigningKey(env.getProperty("token.secret")).parseClaimsJws(jwt).body.subject
        } catch (e: Exception) {
            returnValue = false
        }
        if (subject.isEmpty()) {
            returnValue = false
        }
        return returnValue
    }

    private fun onError(exchange: ServerWebExchange, s: String, httpStatus: HttpStatus): Mono<Void> {
        val response = exchange.response
        response.statusCode = httpStatus

        return response.setComplete()
    }
}