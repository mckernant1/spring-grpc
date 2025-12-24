package org.springframework.grpc.sample

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import org.slf4j.LoggerFactory
import org.springframework.grpc.sample.proto.HelloReply
import org.springframework.grpc.sample.proto.HelloRequest
import org.springframework.grpc.sample.proto.SimpleGrpcKt
import org.springframework.stereotype.Service

@Service
class GrpcServerService : SimpleGrpcKt.SimpleCoroutineImplBase() {

    companion object {
        private val logger = LoggerFactory.getLogger(GrpcServerService::class.java)!!
    }

    override suspend fun sayHello(request: HelloRequest): HelloReply {

        if (KotlinSecurityContextHolder.getContext().authentication == null) {
            logger.error("SecurityContextHolder authentication is null")
            throw RuntimeException(" SecurityContextHolder Authentication not set")
        }

        if (request.name.startsWith("error")) {
            throw IllegalArgumentException("Bad name: ${request.name}")
        }

        if (request.name.startsWith("internal")) {
            throw RuntimeException()
        }

        return HelloReply.newBuilder()
            .setMessage("Hello ==> ${request.name}")
            .build()
    }

    override fun streamHello(request: HelloRequest): Flow<HelloReply> {
        return (1..10)
            .map { HelloReply.newBuilder().setMessage("Hello ($it)  ==> ${request.name}").build() }
            .asFlow()
    }
}
