package br.com.zup.pix.registra

import br.com.zup.KeyRequest
import br.com.zup.KeyServiceGrpc
import br.com.zup.TipoChave.*
import br.com.zup.TipoConta.*
import br.com.zup.integracao.itau.DadosContaResponse
import br.com.zup.integracao.itau.ItauContasClient
import io.grpc.Channel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.http.HttpResponse
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@MicronautTest(transactional = false)
internal class RegistraChaveEndpointTest(
    val chavePixRepository: ChavePixRepository,
    val grpcClient: KeyServiceGrpc.KeyServiceBlockingStub
) {

    @field:Inject
    lateinit var itauContasClient: ItauContasClient

    @Factory
    class Clients {
        @Singleton
        fun blockingStubs(@GrpcChannel(GrpcServerChannel.NAME) channel: Channel): KeyServiceGrpc.KeyServiceBlockingStub {
            return KeyServiceGrpc.newBlockingStub(channel)
        }
    }

    @MockBean(ItauContasClient::class)
    fun itauContasClientMock(): ItauContasClient {
        return Mockito.mock(ItauContasClient::class.java)
    }

    @BeforeEach
    fun setUp() {
        chavePixRepository.deleteAll()
    }

    @Test
    fun `deve adicionar uma nova chave pix por celular para conta corrente`() {

        val request = KeyRequest.newBuilder()
            .setClienteId("c56dfef4-7901-44fb-84e2-a2cefb157890")
            .setTipoChave(CELULAR)
            .setChave("+5511997324364")
            .setTipoConta(CONTA_CORRENTE)
            .build()

        val itauResponse = DadosContaResponse(
            tipo = TipoConta.CONTA_CORRENTE.toString(),
            instituicao = DadosContaResponse.InstituicaoResponse(
                nome = "INSTITUICAO TESTE",
                ispb = "60701190"
            ),
            agencia = "0001",
            numero = "291900",
            titular = DadosContaResponse.TitularResponse(
                id = "c56dfef4-7901-44fb-84e2-a2cefb157890",
                nome = "Titular Teste",
                cpf = "02467781054"
            )
        )

        Mockito.`when`(itauContasClient.buscaContaPorTipo(request.clienteId, request.tipoConta.toString()))
            .thenReturn(HttpResponse.ok(itauResponse))

        val response = grpcClient.cadastrar(request)

        with(response) {
            assertNotNull(pixId)
            assertTrue(chavePixRepository.existsById(UUID.fromString(pixId)))
        }
    }

    @Test
    fun `deve adicionar uma nova chave pix aleatoria para conta poupanca`() {

        val request = KeyRequest.newBuilder()
            .setClienteId("2ac09233-21b2-4276-84fb-d83dbd9f8bab")
            .setTipoChave(ALEATORIA)
            .setTipoConta(CONTA_POUPANCA)
            .build()

        val itauResponse = DadosContaResponse(
            tipo = TipoConta.CONTA_CORRENTE.toString(),
            instituicao = DadosContaResponse.InstituicaoResponse(
                nome = "INSTITUICAO TESTE",
                ispb = "60701190"
            ),
            agencia = "0001",
            numero = "291900",
            titular = DadosContaResponse.TitularResponse(
                id = "2ac09233-21b2-4276-84fb-d83dbd9f8bab",
                nome = "Titular Teste",
                cpf = "83082363083"
            )
        )

        Mockito.`when`(itauContasClient.buscaContaPorTipo(request.clienteId, request.tipoConta.toString()))
            .thenReturn(HttpResponse.ok(itauResponse))

        val response = grpcClient.cadastrar(request)

        with(response) {
            assertNotNull(pixId)
            assertTrue(chavePixRepository.existsById(UUID.fromString(pixId)))
        }
    }

    @Test
    fun `deve adicionar uma nova chave pix por cpf`() {

        val request = KeyRequest.newBuilder()
            .setClienteId("c56dfef4-7901-44fb-84e2-a2cefb157890")
            .setTipoChave(CPF)
            .setChave("02467781054")
            .setTipoConta(CONTA_CORRENTE)
            .build()

        val itauResponse = DadosContaResponse(
            tipo = TipoConta.CONTA_CORRENTE.toString(),
            instituicao = DadosContaResponse.InstituicaoResponse(
                nome = "INSTITUICAO TESTE",
                ispb = "60701190"
            ),
            agencia = "0001",
            numero = "291900",
            titular = DadosContaResponse.TitularResponse(
                id = "c56dfef4-7901-44fb-84e2-a2cefb157890",
                nome = "Titular Teste",
                cpf = "02467781054"
            )
        )

        Mockito.`when`(itauContasClient.buscaContaPorTipo(request.clienteId, request.tipoConta.toString()))
            .thenReturn(HttpResponse.ok(itauResponse))

        val response = grpcClient.cadastrar(request)

        with(response) {
            assertNotNull(pixId)
            assertTrue(chavePixRepository.existsById(UUID.fromString(pixId)))
        }
    }

    @Test
    fun `deve adicionar uma nova chave pix por email`() {

        val request = KeyRequest.newBuilder()
            .setClienteId("2ac09233-21b2-4276-84fb-d83dbd9f8bab")
            .setTipoChave(EMAIL)
            .setChave("teste@email.com")
            .setTipoConta(CONTA_POUPANCA)
            .build()

        val itauResponse = DadosContaResponse(
            tipo = TipoConta.CONTA_CORRENTE.toString(),
            instituicao = DadosContaResponse.InstituicaoResponse(
                nome = "INSTITUICAO TESTE",
                ispb = "60701190"
            ),
            agencia = "0001",
            numero = "291900",
            titular = DadosContaResponse.TitularResponse(
                id = "2ac09233-21b2-4276-84fb-d83dbd9f8bab",
                nome = "Titular Teste",
                cpf = "83082363083"
            )
        )

        Mockito.`when`(itauContasClient.buscaContaPorTipo(request.clienteId, request.tipoConta.toString()))
            .thenReturn(HttpResponse.ok(itauResponse))

        val response = grpcClient.cadastrar(request)

        with(response) {
            assertNotNull(pixId)
            assertTrue(chavePixRepository.existsById(UUID.fromString(pixId)))
        }
    }

    @Test
    fun `nao deve adicionar uma chave pix repetida`() {

        val chaveExistente = ChavePix(
            clienteId = UUID.fromString("2ac09233-21b2-4276-84fb-d83dbd9f8bab"),
            tipoChave = TipoChave.EMAIL,
            chave = "teste@email.com",
            tipoConta = TipoConta.CONTA_POUPANCA,
            conta = ContaAssociada(
                tipo = TipoConta.CONTA_CORRENTE.toString(),
                instituicao = ContaAssociada.Instituicao(
                    nomeInstituicao = "INSTITUICAO TESTE",
                    ispb = "60701190"
                ),
                agencia = "0001",
                numero = "291900",
                titular = ContaAssociada.Titular(
                    titularId = UUID.fromString("2ac09233-21b2-4276-84fb-d83dbd9f8bab"),
                    nomeTitular = "Titular Teste",
                    cpf = "83082363083"
                )
            )
        )

        chavePixRepository.save(chaveExistente)

        val request = KeyRequest.newBuilder()
            .setClienteId("2ac09233-21b2-4276-84fb-d83dbd9f8bab")
            .setTipoChave(EMAIL)
            .setChave("teste@email.com")
            .setTipoConta(CONTA_POUPANCA)
            .build()

        val error = assertThrows<StatusRuntimeException> {
            grpcClient.cadastrar(request)
        }

        with(error) {
            assertEquals(Status.ALREADY_EXISTS.code, status.code)
            assertEquals("Chave Pix ${request.chave} já existente.", status.description)
        }
    }

    @Test
    fun `nao deve adicionar uma nova chave pix com dados de entrada invalidos`() {

        val request = KeyRequest.newBuilder()
            .setClienteId("abcd")
            .setTipoChave(CELULAR)
            .setChave("wxyz")
            .build()

        val error = assertThrows<StatusRuntimeException> {
            grpcClient.cadastrar(request)
        }

        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertTrue(status.description!!.contains("registra.novaChave.tipoConta: must not be null"))
            assertTrue(status.description!!.contains("registra.novaChave: Chave Pix inválida"))
            assertTrue(status.description!!.contains("registra.novaChave.clienteId: must match \"^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}\$\""))
        }
    }

    @Test
    fun `nao deve adicionar uma nova chave pix com chave invalida`() {

        val request = KeyRequest.newBuilder()
            .setClienteId("2ac09233-21b2-4276-84fb-d83dbd9f8bab")
            .setTipoChave(EMAIL)
            .setChave("claramente nao é um e-mail :D")
            .setTipoConta(CONTA_POUPANCA)
            .build()

        val error = assertThrows<StatusRuntimeException> {
            grpcClient.cadastrar(request)
        }

        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("registra.novaChave: Chave Pix inválida", status.description)
        }
    }
}