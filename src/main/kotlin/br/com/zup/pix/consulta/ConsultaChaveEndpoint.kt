package br.com.zup.pix.consulta

import br.com.zup.ConsultaChavePixRequest
import br.com.zup.ConsultaChavePixResponse
import br.com.zup.KeyManagerConsultaServiceGrpc
import br.com.zup.integracao.bcb.BCBPixClient
import br.com.zup.pix.ChavePixRepository
import br.com.zup.shared.grpc.ErrorHandler
import io.grpc.stub.StreamObserver
import io.micronaut.validation.validator.Validator
import javax.inject.Inject
import javax.inject.Singleton

@ErrorHandler
@Singleton
class ConsultaChaveEndpoint(
    @Inject private val repository: ChavePixRepository,
    @Inject private val bcbPixClient: BCBPixClient,
    @Inject private val validator: Validator
) : KeyManagerConsultaServiceGrpc.KeyManagerConsultaServiceImplBase() {

    override fun consulta(
        request: ConsultaChavePixRequest,
        responseObserver: StreamObserver<ConsultaChavePixResponse>
    ) {

        val filtro = request.paraFiltro(validator)
        val chavePixInfo = filtro.filtra(repository = repository, bcbPixClient = bcbPixClient)

        responseObserver.onNext(chavePixInfo.toConsultaChavePixResponse())
        responseObserver.onCompleted();
    }
}