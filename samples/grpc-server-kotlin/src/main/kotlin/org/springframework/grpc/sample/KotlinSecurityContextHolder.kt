package org.springframework.grpc.sample

import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.grpc.sample.KotlinSecurityContextInterceptorConfiguration.KotlinServerInterceptor
import org.springframework.grpc.server.GlobalServerInterceptor
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder

@Configuration
class KotlinSecurityContextInterceptorConfiguration {

    class KotlinServerInterceptor : ServerInterceptor {
        companion object {
            val SECURITY_CONTEXT_KEY: Context.Key<SecurityContext> =
                Context.key("spring-security-context")
        }

        override fun <ReqT, RespT> interceptCall(
            p0: ServerCall<ReqT, RespT>,
            p1: Metadata,
            p2: ServerCallHandler<ReqT, RespT>
        ): ServerCall.Listener<ReqT> {

            val context = SecurityContextHolder.getContext()
            val grpcContext = Context.current()
                .withValue(SECURITY_CONTEXT_KEY, context)

            return Contexts.interceptCall(grpcContext, p0, p1, p2)
        }
    }

    @Bean
    @GlobalServerInterceptor
    fun kotlinSecurityContextInterceptor(): ServerInterceptor {
        return KotlinServerInterceptor()
    }
}

object KotlinSecurityContextHolder {

    fun getContext(): SecurityContext {
        return KotlinServerInterceptor.SECURITY_CONTEXT_KEY.get()
    }

}
