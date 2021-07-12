package br.com.zup.pix.consulta

import br.com.zup.ConsultaChavePixRequest
import br.com.zup.ConsultaChavePixRequest.FiltroPorPixId
import br.com.zup.KeyManagerConsultaServiceGrpc
import br.com.zup.integracao.bcb.*
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


@MicronautTest(transactional = false)
internal class ConsultaChaveEndpointTest(
    private val chavePixRepository: ChavePixRepository,
    private val grpcClient: KeyManagerConsultaServiceGrpc.KeyManagerConsultaServiceBlockingStub
) {

    @field:Inject
    lateinit var bcbPixClient: BCBPixClient

    companion object {
        val CLIENTE_ID = UUID.randomUUID()
    }

    @Factory
    class Clients {
        @Singleton
        fun blockingStubs(@GrpcChannel(GrpcServerChannel.NAME) channel: Channel): KeyManagerConsultaServiceGrpc.KeyManagerConsultaServiceBlockingStub {
            return KeyManagerConsultaServiceGrpc.newBlockingStub(channel)
        }
    }

    @MockBean(BCBPixClient::class)
    fun bcbPixClientMock(): BCBPixClient {
        return Mockito.mock(BCBPixClient::class.java)
    }

    @BeforeEach
    fun setUp() {
        chavePixRepository.deleteAll()
        chavePixRepository.save(
            chavePix(
                tipoChave = TipoChave.EMAIL,
                chave = "tester@email.com",
                clienteId = CLIENTE_ID
            )
        )
        chavePixRepository.save(
            chavePix(
                tipoChave = TipoChave.CELULAR,
                chave = "+5511987356283",
                clienteId = CLIENTE_ID
            )
        )
        chavePixRepository.save(
            chavePix(
                tipoChave = TipoChave.ALEATORIA,
                chave = UUID.randomUUID().toString(),
                clienteId = CLIENTE_ID
            )
        )
        chavePixRepository.save(chavePix(tipoChave = TipoChave.CPF, chave = "17956629055", clienteId = CLIENTE_ID))
    }

    @Test
    internal fun `deve retornar chave por pixId e clienteId`() {
        val chaveExistente = chavePixRepository.findByChave("tester@email.com").get()

        val request = ConsultaChavePixRequest.newBuilder()
            .setPixId(
                FiltroPorPixId.newBuilder()
                    .setPixId(chaveExistente.id.toString())
                    .setClienteId(chaveExistente.clienteId.toString())
                    .build()
            ).build()

        val response = grpcClient.consulta(request)

        with(response) {
            assertEquals(chaveExistente.id.toString(), this.pixId)
            assertEquals(chaveExistente.clienteId.toString(), this.clienteId)
            assertEquals(chaveExistente.tipoChave.name, this.chave.tipo.name)
            assertEquals(chaveExistente.chave, this.chave.chave)
        }
    }

    @Test
    internal fun `nao deve retornar chave por pixId e clienteId quando filtro invalido`() {
        val request = ConsultaChavePixRequest.newBuilder()
            .setPixId(
                FiltroPorPixId.newBuilder()
                    .setPixId("")
                    .setClienteId("")
                    .build()
            ).build()

        val error = assertThrows<StatusRuntimeException> {
            grpcClient.consulta(request)
        }

        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Dados inválidos", status.description)
            val violations = getViolations(this)
            assertTrue(violations.contains(Pair("pixId", "não deve estar em branco")))
            assertTrue(violations.contains(Pair("pixId", "Não é um formato válido de UUID")))
            assertTrue(violations.contains(Pair("clientId", "não deve estar em branco")))
            assertTrue(violations.contains(Pair("clientId", "Não é um formato válido de UUID")))
        }
    }

    @Test
    internal fun `nao deve retornar chave por pixId e clienteId quando registro nao existir`() {
        val request = ConsultaChavePixRequest.newBuilder()
            .setPixId(
                FiltroPorPixId.newBuilder()
                    .setPixId(UUID.randomUUID().toString())
                    .setClienteId(UUID.randomUUID().toString())
                    .build()
            ).build()

        val error = assertThrows<StatusRuntimeException> {
            grpcClient.consulta(request)
        }

        with(error) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Chave Pix não encontrada", status.description)
        }
    }

    @Test
    internal fun `deve retornar chave por valor da chave quando registro existir localmente`() {
        val chaveExistente = chavePixRepository.findByChave("+5511987356283").get()

        val request = ConsultaChavePixRequest.newBuilder()
            .setChave(chaveExistente.chave)
            .build()

        val response = grpcClient.consulta(request)

        with(response) {
            assertEquals(chaveExistente.id.toString(), this.pixId)
            assertEquals(chaveExistente.clienteId.toString(), this.clienteId)
            assertEquals(chaveExistente.tipoChave.name, this.chave.tipo.name)
            assertEquals(chaveExistente.chave, this.chave.chave)
        }
    }

    @Test
    internal fun `deve retornar chave por valor da chave quando registro nao existir localmente mas existir no BCB`() {
        val chave = "62114509079"

        val request = ConsultaChavePixRequest.newBuilder()
            .setChave(chave)
            .build()

        Mockito.`when`(bcbPixClient.buscaChave(chave))
            .thenReturn(HttpResponse.ok(bcbResponse()))

        val response = grpcClient.consulta(request)

        with(response) {
            assertEquals("", this.pixId)
            assertEquals("", this.clienteId)
            assertEquals(bcbResponse().key, this.chave.chave)
            assertEquals(TipoChave.CPF.name, this.chave.tipo.name)
        }
    }

    @Test
    internal fun `nao deve retornar chave por valor da chave quando registro nao existir localmente nem no BCB`() {
        val chave = "emailquenaoexiste@email.com"

        val request = ConsultaChavePixRequest.newBuilder()
            .setChave(chave)
            .build()

        Mockito.`when`(bcbPixClient.buscaChave(chave))
            .thenReturn(HttpResponse.notFound())

        val error = assertThrows<StatusRuntimeException> {
            grpcClient.consulta(request)
        }

        with(error) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Chave Pix não encontrada", status.description)
        }
    }

    @Test
    internal fun `nao deve retornar chave por valor da chave quando filtro invalido`() {
        val request = ConsultaChavePixRequest.newBuilder()
            .setChave("")
            .build()

        val error = assertThrows<StatusRuntimeException> {
            grpcClient.consulta(request)
        }

        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Dados inválidos", status.description)
            val violations = getViolations(this)
            assertTrue(violations.contains(Pair("chave", "não deve estar em branco")))
        }
    }

    @Test
    internal fun `nao deve retornar chave quando filtro invalido`() {
        val request = ConsultaChavePixRequest.newBuilder().build()

        val error = assertThrows<StatusRuntimeException> {
            grpcClient.consulta(request)
        }

        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Chave Pix inválida ou não informada", status.description)
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

    fun bcbResponse(): PixKeyDetailsResponse {
        return PixKeyDetailsResponse(
            keyType = PixKeyType.CPF,
            key = "62114509079",
            bankAccount = BankAccount(
                participant = "60701190",
                branch = "0001",
                accountNumber = "291900",
                accountType = AccountType.CACC
            ),
            owner = Owner(
                type = Owner.OwnerType.NATURAL_PERSON,
                name = "Titular Teste",
                taxIdNumber = "62114509079"
            ),
            createdAt = LocalDateTime.now().minusDays(3)
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