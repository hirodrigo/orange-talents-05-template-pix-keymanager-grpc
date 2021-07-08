package br.com.zup.pix.remove

import br.com.zup.KeyManagerRemoveServiceGrpc
import br.com.zup.RemoveChavePixRequest
import br.com.zup.integracao.bcb.BCBPixClient
import br.com.zup.integracao.bcb.DeletePixKeyRequest
import br.com.zup.integracao.bcb.DeletePixKeyResponse
import br.com.zup.pix.ChavePixRepository
import br.com.zup.pix.registra.*
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
internal class RemoveChaveEndpointTest(
    private val chavePixRepository: ChavePixRepository,
    private val grpcClient: KeyManagerRemoveServiceGrpc.KeyManagerRemoveServiceBlockingStub
) {

    @field:Inject
    lateinit var bcbPixClient: BCBPixClient

    private lateinit var chaveExistente: ChavePix

    @Factory
    class Clients {
        @Singleton
        fun blockingStubs(@GrpcChannel(GrpcServerChannel.NAME) channel: Channel): KeyManagerRemoveServiceGrpc.KeyManagerRemoveServiceBlockingStub {
            return KeyManagerRemoveServiceGrpc.newBlockingStub(channel)
        }
    }

    @MockBean(BCBPixClient::class)
    fun bcbPixClientMock(): BCBPixClient {
        return Mockito.mock(BCBPixClient::class.java)
    }

    @BeforeEach
    fun setUp() {
        chavePixRepository.deleteAll()
        chaveExistente = chavePix(
            tipoChave = TipoChave.EMAIL,
            chave = "teste@teste.com",
            clienteId = UUID.randomUUID()
        )
        chavePixRepository.save(chaveExistente)
    }

    @Test
    internal fun `deve remover chave pix com sucesso`() {
        val request = RemoveChavePixRequest.newBuilder()
            .setClienteId(chaveExistente.clienteId.toString())
            .setPixId(chaveExistente.id.toString())
            .build()

        Mockito.`when`(
            bcbPixClient.removeChave(
                key = chaveExistente.chave,
                request = DeletePixKeyRequest(chaveExistente.chave)
            )
        ).thenReturn(
            HttpResponse.ok(
                DeletePixKeyResponse(
                    key = chaveExistente.chave,
                    participant = ContaAssociada.ITAU_UNIBANCO_ISBP,
                    deletedAt = LocalDateTime.now()
                )
            )
        )

        val response = grpcClient.remove(request)

        with(response) {
            val possivelChavePix = chavePixRepository.findById(chaveExistente.id!!)
            assertTrue(possivelChavePix.isEmpty)
            assertEquals(chaveExistente.id.toString(), pixId)
        }
    }

    @Test
    internal fun `nao deve remover chave pix de outro cliente`() {
        val request = RemoveChavePixRequest.newBuilder()
            .setClienteId(UUID.randomUUID().toString())
            .setPixId(chaveExistente.id.toString())
            .build()

        val error = assertThrows<StatusRuntimeException> {
            grpcClient.remove(request)
        }

        with(error) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Chave Pix não encontrada ou não pertence ao cliente.", status.description)
        }

        assertEquals(1, chavePixRepository.count())
    }

    @Test
    internal fun `nao deve remover chave pix inexistente`() {
        val request = RemoveChavePixRequest.newBuilder()
            .setClienteId(chaveExistente.clienteId.toString())
            .setPixId(UUID.randomUUID().toString())
            .build()

        val error = assertThrows<StatusRuntimeException> {
            grpcClient.remove(request)
        }

        with(error) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Chave Pix não encontrada ou não pertence ao cliente.", status.description)
        }

        assertEquals(1, chavePixRepository.count())
    }


    @Test
    internal fun `nao deve aceitar requests com parametros invalidos`() {
        val request = RemoveChavePixRequest.newBuilder()
            .setClienteId("abc")
            .setPixId("123")
            .build()

        val error = assertThrows<StatusRuntimeException> {
            grpcClient.remove(request)
        }

        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Dados inválidos", status.description)
            val violations = getViolations(this)
            assertTrue(
                violations.contains(
                    Pair(
                        "pixId",
                        "must match \"^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}\$\""
                    )
                )
            )
            assertTrue(
                violations.contains(
                    Pair(
                        "clienteId",
                        "must match \"^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}\$\""
                    )
                )
            )
        }
    }

    @Test
    internal fun `nao deve remover uma chave pix quando nao conseguir remove-la do servico BCB`() {
        val request = RemoveChavePixRequest.newBuilder()
            .setClienteId(chaveExistente.clienteId.toString())
            .setPixId(chaveExistente.id.toString())
            .build()

        Mockito.`when`(
            bcbPixClient.removeChave(
                key = chaveExistente.chave,
                request = DeletePixKeyRequest(chaveExistente.chave)
            )
        ).thenReturn(HttpResponse.unprocessableEntity())

        val error = assertThrows<StatusRuntimeException> {
            grpcClient.remove(request)
        }

        with(error) {
            assertEquals(Status.FAILED_PRECONDITION.code, status.code)
            assertEquals("Falha ao remover chave no Banco Central", status.description)
        }

        assertEquals(1, chavePixRepository.count())
    }

    private fun chavePix(
        tipoChave: TipoChave,
        chave: String = UUID.randomUUID().toString(),
        clienteId: UUID = RegistraChaveEndpointTest.CLIENTE_ID
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