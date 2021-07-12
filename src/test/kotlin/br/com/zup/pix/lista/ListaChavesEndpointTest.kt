package br.com.zup.pix.lista

import br.com.zup.KeyManagerListaServiceGrpc
import br.com.zup.ListaChavesPixRequest
import br.com.zup.TipoChave.CELULAR
import br.com.zup.TipoChave.EMAIL
import br.com.zup.pix.*
import br.com.zup.pix.consulta.ConsultaChaveEndpointTest
import com.google.rpc.BadRequest
import io.grpc.Channel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.protobuf.StatusProto
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*
import javax.inject.Singleton

@MicronautTest(transactional = false)
internal class ListaChavesEndpointTest(
    private val chavePixRepository: ChavePixRepository,
    private val grpcClient: KeyManagerListaServiceGrpc.KeyManagerListaServiceBlockingStub
) {

    companion object {
        val CLIENTE1_ID = UUID.randomUUID()
        val CLIENTE2_ID = UUID.randomUUID()
        val CHAVE_ALEATORIA = UUID.randomUUID()
    }

    @Factory
    class Clients {
        @Singleton
        fun blockingStubs(@GrpcChannel(GrpcServerChannel.NAME) channel: Channel): KeyManagerListaServiceGrpc.KeyManagerListaServiceBlockingStub {
            return KeyManagerListaServiceGrpc.newBlockingStub(channel)
        }
    }

    @BeforeEach
    fun setUp() {
        chavePixRepository.deleteAll()
        chavePixRepository.save(
            chavePix(tipoChave = TipoChave.EMAIL, chave = "tester@email.com", clienteId = CLIENTE1_ID)
        )
        chavePixRepository.save(
            chavePix(tipoChave = TipoChave.CELULAR, chave = "+5511987356283", clienteId = CLIENTE1_ID)
        )
        chavePixRepository.save(
            chavePix(tipoChave = TipoChave.ALEATORIA, chave = CHAVE_ALEATORIA.toString(), clienteId = CLIENTE2_ID)
        )
        chavePixRepository.save(
            chavePix(tipoChave = TipoChave.CPF, chave = "17956629055", clienteId = CLIENTE2_ID)
        )
    }

    @Test
    internal fun `deve retornar todas as chaves pix do cliente`() {
        val request = ListaChavesPixRequest.newBuilder()
            .setClienteId(CLIENTE1_ID.toString())
            .build()

        val response = grpcClient.lista(request)

        with(response) {
            assertEquals(CLIENTE1_ID.toString(), this.clienteId)
            assertTrue(this.chavesList.size == 2)
            assertTrue(
                this.chavesList.map {
                    Pair(it.tipoChave, it.chave)
                }.toList().containsAll(
                    listOf<Pair<Any, Any>>(
                        Pair(EMAIL, "tester@email.com"),
                        Pair(CELULAR, "+5511987356283")
                    )
                )
            )
        }
    }

    @Test
    internal fun `nao deve retornar chaves quando cliente nao possuir chaves`() {

        val clienteId = UUID.randomUUID().toString()

        val request = ListaChavesPixRequest.newBuilder()
            .setClienteId(clienteId)
            .build()

        val response = grpcClient.lista(request)

        with(response) {
            assertEquals(clienteId, this.clienteId)
            assertTrue(this.chavesList.size == 0)
        }
    }

    @Test
    internal fun `nao deve retornar chaves quando clienteId for invalido`() {

        val request = ListaChavesPixRequest.newBuilder()
            .setClienteId("")
            .build()

        val error = assertThrows<StatusRuntimeException> {
            grpcClient.lista(request)
        }

        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Dados inválidos", status.description)
            val violations = getViolations(this)
            assertTrue(violations.contains(Pair("clienteId", "não deve estar em branco")))
            assertTrue(violations.contains(Pair("clienteId", "Não é um formato válido de UUID")))
        }
    }

    private fun chavePix(
        tipoChave: TipoChave,
        chave: String = UUID.randomUUID().toString(),
        clienteId: UUID = ConsultaChaveEndpointTest.CLIENTE_ID
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