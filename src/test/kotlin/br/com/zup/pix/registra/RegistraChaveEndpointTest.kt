package br.com.zup.pix.registra

import br.com.zup.KeyManagerRegistraServiceGrpc
import br.com.zup.RegistraChavePixRequest
import br.com.zup.TipoChave.CELULAR
import br.com.zup.TipoChave.EMAIL
import br.com.zup.TipoConta.CONTA_CORRENTE
import br.com.zup.TipoConta.CONTA_POUPANCA
import br.com.zup.integracao.bcb.*
import br.com.zup.integracao.itau.DadosContaResponse
import br.com.zup.integracao.itau.ItauContasClient
import br.com.zup.pix.*
import com.google.rpc.BadRequest
import io.grpc.Channel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.protobuf.StatusProto
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
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@MicronautTest(transactional = false)
internal class RegistraChaveEndpointTest(
    val chavePixRepository: ChavePixRepository,
    val grpcClient: KeyManagerRegistraServiceGrpc.KeyManagerRegistraServiceBlockingStub
) {

    @field:Inject
    lateinit var itauContasClient: ItauContasClient

    @field:Inject
    lateinit var bcbPixClient: BCBPixClient

    companion object {
        val CLIENTE_ID = UUID.randomUUID()
    }

    @Factory
    class Clients {
        @Singleton
        fun blockingStubs(@GrpcChannel(GrpcServerChannel.NAME) channel: Channel): KeyManagerRegistraServiceGrpc.KeyManagerRegistraServiceBlockingStub {
            return KeyManagerRegistraServiceGrpc.newBlockingStub(channel)
        }
    }

    @MockBean(ItauContasClient::class)
    fun itauContasClientMock(): ItauContasClient {
        return Mockito.mock(ItauContasClient::class.java)
    }

    @MockBean(BCBPixClient::class)
    fun bcbPixClientMock(): BCBPixClient {
        return Mockito.mock(BCBPixClient::class.java)
    }

    @BeforeEach
    fun setUp() {
        chavePixRepository.deleteAll()
    }

    @Test
    fun `deve adicionar uma nova chave pix com sucesso`() {

        val grpcRequest = RegistraChavePixRequest.newBuilder()
            .setClienteId(CLIENTE_ID.toString())
            .setTipoChave(CELULAR)
            .setChave("+5511974348765")
            .setTipoConta(CONTA_CORRENTE)
            .build()

        Mockito.`when`(
            itauContasClient.buscaContaPorTipo(
                clienteId = grpcRequest.clienteId,
                tipo = grpcRequest.tipoConta.toString()
            )
        ).thenReturn(HttpResponse.ok(itauResponse()))

        Mockito.`when`(
            bcbPixClient.criaNovaChave(bcbRequest())
        ).thenReturn(HttpResponse.created(bcbResponse()))

        val response = grpcClient.cadastrar(grpcRequest)

        with(response) {
            assertNotNull(pixId)
            assertTrue(chavePixRepository.existsById(UUID.fromString(pixId)))
        }
    }

    @Test
    fun `nao deve adicionar uma chave pix repetida`() {

        val chaveExistente = chavePix(
            tipoChave = TipoChave.CELULAR,
            chave = "+5511974348765",
            clienteId = CLIENTE_ID
        )

        chavePixRepository.save(chaveExistente)

        val grpcRequest = RegistraChavePixRequest.newBuilder()
            .setClienteId(CLIENTE_ID.toString())
            .setTipoChave(CELULAR)
            .setChave("+5511974348765")
            .setTipoConta(CONTA_POUPANCA)
            .build()

        val error = assertThrows<StatusRuntimeException> {
            grpcClient.cadastrar(grpcRequest)
        }

        with(error) {
            assertEquals(Status.ALREADY_EXISTS.code, status.code)
            assertEquals("Chave Pix ${grpcRequest.chave} já existente.", status.description)
        }
    }

    @Test
    fun `nao deve adicionar uma nova chave pix com dados de entrada invalidos`() {

        val grpcRequest = RegistraChavePixRequest.newBuilder()
            .setClienteId("abcd")
            .setTipoChave(CELULAR)
            .setChave("wxyz")
            .build()

        val error = assertThrows<StatusRuntimeException> {
            grpcClient.cadastrar(grpcRequest)
        }

        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Dados inválidos", status.description)
            val violations = getViolations(this)
            assertTrue(violations.contains(Pair("tipoConta", "não deve ser nulo")))
            assertTrue(violations.contains(Pair("chave", "Chave Pix inválida (CELULAR)")))
        }
    }

    @Test
    fun `nao deve adicionar uma nova chave pix com chave invalida`() {

        val grpcRequest = RegistraChavePixRequest.newBuilder()
            .setClienteId(CLIENTE_ID.toString())
            .setTipoChave(EMAIL)
            .setChave("claramente nao é um e-mail :D")
            .setTipoConta(CONTA_POUPANCA)
            .build()

        val error = assertThrows<StatusRuntimeException> {
            grpcClient.cadastrar(grpcRequest)
        }

        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Dados inválidos", status.description)
            val violations = getViolations(this)
            assertTrue(violations.contains(Pair("chave", "Chave Pix inválida (EMAIL)")))
        }
    }

    @Test
    internal fun `nao deve adicionar uma nova chave pix quando nao encontrar dados da conta no servico Itau`() {
        val grpcRequest = RegistraChavePixRequest.newBuilder()
            .setClienteId(CLIENTE_ID.toString())
            .setTipoChave(EMAIL)
            .setChave("email@email.com")
            .setTipoConta(CONTA_POUPANCA)
            .build()

        Mockito.`when`(
            itauContasClient.buscaContaPorTipo(
                clienteId = grpcRequest.clienteId,
                tipo = grpcRequest.tipoConta.toString()
            )
        ).thenReturn(HttpResponse.notFound())

        val error = assertThrows<StatusRuntimeException> {
            grpcClient.cadastrar(grpcRequest)
        }

        with(error) {
            assertEquals(Status.FAILED_PRECONDITION.code, status.code)
            assertEquals("Cliente não encontrado no Itaú.", status.description)
        }

    }

    @Test
    internal fun `nao deve adicionar uma nova chave pix quando nao conseguir adiciona-la no servico BCB`() {
        val grpcRequest = RegistraChavePixRequest.newBuilder()
            .setClienteId(CLIENTE_ID.toString())
            .setTipoChave(CELULAR)
            .setChave("+5511974348765")
            .setTipoConta(CONTA_CORRENTE)
            .build()

        Mockito.`when`(
            itauContasClient.buscaContaPorTipo(
                clienteId = grpcRequest.clienteId,
                tipo = grpcRequest.tipoConta.toString()
            )
        ).thenReturn(HttpResponse.ok(itauResponse()))

        Mockito.`when`(
            bcbPixClient.criaNovaChave(bcbRequest())
        ).thenReturn(HttpResponse.badRequest())

        val error = assertThrows<StatusRuntimeException> {
            grpcClient.cadastrar(grpcRequest)
        }

        with(error) {
            assertEquals(Status.FAILED_PRECONDITION.code, status.code)
            assertEquals("Falha ao cadastrar chave no Banco Central", status.description)
        }
    }


    private fun chavePix(
        tipoChave: TipoChave,
        chave: String = UUID.randomUUID().toString(),
        clienteId: UUID = CLIENTE_ID
    ): ChavePix {
        return ChavePix(
            clienteId = clienteId,
            tipoChave = tipoChave,
            chave = chave,
            tipoConta = TipoConta.CONTA_CORRENTE,
            conta = ContaAssociada(
                tipo = TipoConta.CONTA_CORRENTE.toString(),
                instituicao = ContaAssociada.Instituicao(
                    nomeInstituicao = "INSTITUICAO TESTE",
                    ispb = "60701190"
                ),
                agencia = "0001",
                numero = "291900",
                titular = ContaAssociada.Titular(
                    titularId = clienteId,
                    nomeTitular = "Titular Teste",
                    cpf = "83082363083"
                )
            )
        )
    }

    private fun itauResponse(): DadosContaResponse {
        return DadosContaResponse(
            tipo = TipoConta.CONTA_CORRENTE.toString(),
            instituicao = DadosContaResponse.InstituicaoResponse(
                nome = "INSTITUICAO TESTE",
                ispb = "60701190"
            ),
            agencia = "0001",
            numero = "291900",
            titular = DadosContaResponse.TitularResponse(
                id = CLIENTE_ID.toString(),
                nome = "Titular Teste",
                cpf = "02467781054"
            )
        )
    }

    private fun bcbRequest(): CreatePixKeyRequest {
        return CreatePixKeyRequest(
            keyType = PixKeyType.PHONE,
            key = "+5511974348765",
            bankAccount = bankAccount(),
            owner = owner(),
        )
    }

    private fun bcbResponse(): CreatePixKeyResponse {
        return CreatePixKeyResponse(
            keyType = PixKeyType.PHONE,
            key = "+5511974348765",
            bankAccount = bankAccount(),
            owner = owner(),
            createdAt = LocalDateTime.now()
        )
    }

    private fun bankAccount(): BankAccount {
        return BankAccount(
            participant = "60701190",
            branch = "0001",
            accountNumber = "291900",
            accountType = AccountType.CACC
        )
    }

    private fun owner(): Owner {
        return Owner(
            type = Owner.OwnerType.NATURAL_PERSON,
            name = "Titular Teste",
            taxIdNumber = "02467781054"
        )
    }

    private fun getViolations(e: StatusRuntimeException): List<Pair<String, String>> {
        val details = StatusProto.fromThrowable(e)
            ?.detailsList?.get(0)!!
            .unpack(BadRequest::class.java)

        return details.fieldViolationsList
            .map {
                it.field to it.description
            }
    }
}