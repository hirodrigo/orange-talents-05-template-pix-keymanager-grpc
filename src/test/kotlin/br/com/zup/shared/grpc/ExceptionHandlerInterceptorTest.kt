package br.com.zup.shared.grpc

import io.grpc.BindableService
import io.grpc.stub.StreamObserver
import io.micronaut.aop.MethodInvocationContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
internal class ExceptionHandlerInterceptorTest {

    @Mock
    lateinit var context: MethodInvocationContext<BindableService, Any?>

    val interceptor = ExceptionHandlerInterceptor(resolver = ExceptionHandlerResolver(handlers = emptyList()))

    @Test
    fun `deve capturar a excecao lancada pelo execucao do metodo e gerar um erro na resposta grpc`(@Mock streamObserver: StreamObserver<*>) {
        with(context) {
            Mockito.`when`(proceed()).thenThrow(RuntimeException("Qualquer mensagem de Exception"))
            Mockito.`when`(parameterValues).thenReturn(arrayOf(null, streamObserver))
        }

        interceptor.intercept(context)

        Mockito.verify(streamObserver).onError(Mockito.notNull())
    }

    @Test
    fun `nao deve gerar um erro na resposta grpc quando nenhuma excecao for lancada`() {
        val expected = "Isso não é uma exceção"
        Mockito.`when`(context.proceed()).thenReturn(expected)
        assertEquals(expected, interceptor.intercept(context))
    }
}