package br.com.zup.pix.remove

import br.com.zup.KeyManagerRemoveServiceGrpc
import br.com.zup.RemoveChavePixRequest
import br.com.zup.pix.ChavePixRepository
import br.com.zup.pix.registra.ChavePix
import br.com.zup.pix.registra.ContaAssociada
import br.com.zup.pix.registra.TipoChave
import br.com.zup.pix.registra.TipoConta
import io.grpc.Channel
import io.grpc.Status
import io.grpc.StatusRuntimeException
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
internal class RemoveChaveEndpointTest(
    val chavePixRepository: ChavePixRepository,
    val grpcClient: KeyManagerRemoveServiceGrpc.KeyManagerRemoveServiceBlockingStub
) {

    @Factory
    class Clients {
        @Singleton
        fun blockingStubs(@GrpcChannel(GrpcServerChannel.NAME) channel: Channel): KeyManagerRemoveServiceGrpc.KeyManagerRemoveServiceBlockingStub {
            return KeyManagerRemoveServiceGrpc.newBlockingStub(channel)
        }
    }

    @BeforeEach
    fun setUp() {
        chavePixRepository.deleteAll()
    }

    @Test
    internal fun `deve remover chave pix com sucesso`() {
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
        assertEquals(1, chavePixRepository.count())

        val request = RemoveChavePixRequest.newBuilder()
            .setClienteId("2ac09233-21b2-4276-84fb-d83dbd9f8bab")
            .setPixId(chaveExistente.id.toString())
            .build()

        val response = grpcClient.remove(request)

        with(response) {
            val possivelChavePix = chavePixRepository.findById(UUID.fromString(pixId))
            assertTrue(possivelChavePix.isEmpty)
            assertEquals(0, chavePixRepository.count())
        }
    }

    @Test
    internal fun `nao deve remover chave pix de outro cliente`() {
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
        assertEquals(1, chavePixRepository.count())

        val request = RemoveChavePixRequest.newBuilder()
            .setClienteId("c56dfef4-7901-44fb-84e2-a2cefb157890")
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
            assertTrue(status.description!!.contains("remove.dto.pixId: must match \"^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}\$\""))
            assertTrue(status.description!!.contains("remove.dto.clienteId: must match \"^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}\$\""))
        }
    }
}