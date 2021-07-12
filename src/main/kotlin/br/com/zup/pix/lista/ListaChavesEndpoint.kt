package br.com.zup.pix.lista

import br.com.zup.*
import br.com.zup.pix.ChavePixRepository
import br.com.zup.shared.grpc.ErrorHandler
import com.google.protobuf.Timestamp
import io.grpc.stub.StreamObserver
import io.micronaut.validation.validator.Validator
import java.time.ZoneId
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@ErrorHandler
@Singleton
class ListaChavesEndpoint(
    @Inject private val validator: Validator,
    @Inject private val chavePixRepository: ChavePixRepository
) : KeyManagerListaServiceGrpc.KeyManagerListaServiceImplBase() {

    override fun lista(request: ListaChavesPixRequest, responseObserver: StreamObserver<ListaChavesPixResponse>) {

        val listaChavesDto = request.paraDto(validator)

        val chaves = chavePixRepository.findAllByClienteId(
            UUID.fromString(listaChavesDto.clienteId)
        ).map { chave ->
            ListaChavesPixResponse.ChavePix.newBuilder()
                .setPixId(chave.id.toString())
                .setTipoChave(TipoChave.valueOf(chave.tipoChave.name))
                .setChave(chave.chave)
                .setTipoConta(TipoConta.valueOf(chave.tipoConta.name))
                .setCriadaEm(chave.criadaEm.let { criadaEm ->
                    val instant = criadaEm.atZone(ZoneId.of("UTC")).toInstant()
                    Timestamp.newBuilder()
                        .setSeconds(instant.epochSecond)
                        .setNanos(instant.nano)
                        .build()
                })
                .build()
        }

        responseObserver.onNext(
            ListaChavesPixResponse.newBuilder()
                .setClienteId(listaChavesDto.clienteId)
                .addAllChaves(chaves)
                .build()
        )

        responseObserver.onCompleted();
    }
}