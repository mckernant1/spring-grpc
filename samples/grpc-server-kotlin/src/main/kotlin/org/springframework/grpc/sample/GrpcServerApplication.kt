package org.springframework.grpc.sample

import io.grpc.Status
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.grpc.server.GlobalServerInterceptor
import org.springframework.grpc.server.exception.GrpcExceptionHandler
import org.springframework.grpc.server.security.AuthenticationProcessInterceptor
import org.springframework.grpc.server.security.GrpcSecurity
import org.springframework.security.config.Customizer.withDefaults

@SpringBootApplication
open class GrpcServerApplication {

    @Bean
    open fun globalInterceptor(): GrpcExceptionHandler = GrpcExceptionHandler { exception ->
        when (exception) {
            is IllegalArgumentException -> Status.INVALID_ARGUMENT.withDescription(exception.message).asException();
            else -> null
        }
    }

    @Bean
    @GlobalServerInterceptor
    @Throws(Exception::class)
    open fun jwtSecurityFilterChain(grpc: GrpcSecurity): AuthenticationProcessInterceptor {
        return grpc
            .authorizeRequests { requests ->
                requests.methods("Simple/StreamHello")
                    .hasAuthority("SCOPE_profile")

                requests.methods("Simple/SayHello")
                    .authenticated()
                    .methods("grpc.*/*")
                    .permitAll()
                requests.allRequests()
                    .denyAll()
            }
            .oauth2ResourceServer { resourceServer ->
                resourceServer.jwt(withDefaults())
            }
            .build()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<GrpcServerApplication>(*args)
        }
    }
}


