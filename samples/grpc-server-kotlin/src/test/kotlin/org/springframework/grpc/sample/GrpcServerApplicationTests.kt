package org.springframework.grpc.sample

import io.grpc.reflection.v1.ServerReflectionGrpc
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.SpringApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.experimental.boot.server.exec.ClasspathBuilder
import org.springframework.experimental.boot.server.exec.CommonsExecWebServerFactoryBean
import org.springframework.experimental.boot.server.exec.MavenClasspathEntry
import org.springframework.experimental.boot.test.context.EnableDynamicProperty
import org.springframework.experimental.boot.test.context.OAuth2ClientProviderIssuerUri
import org.springframework.grpc.client.GrpcChannelBuilderCustomizer
import org.springframework.grpc.client.ImportGrpcClients
import org.springframework.grpc.client.interceptor.security.BearerTokenAuthenticationInterceptor
import org.springframework.grpc.sample.proto.HelloRequest
import org.springframework.grpc.sample.proto.SimpleGrpc.SimpleBlockingStub
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest
import org.springframework.security.oauth2.client.endpoint.RestClientClientCredentialsTokenResponseClient
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.test.annotation.DirtiesContext

@SpringBootTest(
    properties = [
        "spring.grpc.server.port=0",
        "spring.grpc.client.default-channel.address=0.0.0.0:\${local.grpc.port}"
    ],
    classes = [
        GrpcServerApplication::class,
        GrpcServerApplicationTests.ExtraConfiguration::class
    ]
)
@DirtiesContext
class GrpcServerApplicationTests {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.from(GrpcServerApplication::main)
                .with(ExtraConfiguration::class.java).run(*args)
        }
    }

    private val log: Log = LogFactory.getLog(this.javaClass)

    @TestConfiguration(proxyBeanMethods = false)
    @EnableDynamicProperty
    @ImportGrpcClients(
        target = "stub",
        types = [
            SimpleBlockingStub::class,
            ServerReflectionGrpc.ServerReflectionStub::class
        ]
    )
    @ImportGrpcClients(
        target = "secure",
        prefix = "secure",
        types = [SimpleBlockingStub::class]
    )
    class ExtraConfiguration {
        private var token: String? = null

        @Bean
        fun stubs(context: ObjectProvider<ClientRegistrationRepository>): GrpcChannelBuilderCustomizer<*> {
            return GrpcChannelBuilderCustomizer.matching(
                "secure"
            ) { builder ->
                builder.intercept(
                    BearerTokenAuthenticationInterceptor
                    { token(context)!! }
                )
            }
        }

        private fun token(context: ObjectProvider<ClientRegistrationRepository>): String? {
            if (this.token == null) { // ... plus we could check for expiry
                val creds = RestClientClientCredentialsTokenResponseClient()
                val registry = context.getObject()
                val reg = registry.findByRegistrationId("spring")
                this.token = creds.getTokenResponse(OAuth2ClientCredentialsGrantRequest(reg))
                    .getAccessToken()
                    .getTokenValue()
            }
            return this.token
        }


        @Bean
        @OAuth2ClientProviderIssuerUri
        fun authServer(): CommonsExecWebServerFactoryBean? {
            return CommonsExecWebServerFactoryBean.builder()
                .useGenericSpringBootMain()
                .classpath { classpath: ClasspathBuilder ->
                    classpath.entries(MavenClasspathEntry.springBootStarter("oauth2-authorization-server"))
                }
        }

    }

    @Autowired
    @Qualifier("secureSimpleBlockingStub")
    private lateinit var stub: SimpleBlockingStub

    @Test
    fun contextLoads() {
    }

    @Test
    fun serverResponds() {
        log.info("Testing")
        val response = stub.sayHello(
            HelloRequest.newBuilder()
                .setName("Alien")
                .build()
        )
        Assertions.assertEquals("Hello ==> Alien", response.getMessage())
    }
}
