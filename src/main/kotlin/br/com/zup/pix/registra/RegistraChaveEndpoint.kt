package br.com.zup.pix.registra

import br.com.zup.*
import br.com.zup.shared.grpc.ErrorHandler
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import javax.inject.Singleton

@ErrorHandler
@Singleton
class RegistraChaveEndpoint(
    @Inject val registraChave: RegistraChave
) : KeyManagerRegistraServiceGrpc.KeyManagerRegistraServiceImplBase() {

    override fun cadastrar(request: RegistraChavePixRequest, responseObserver: StreamObserver<RegistraChavePixResponse>) {
        val novaChavePix = request.paraNovaChavePix()
        val chaveCriada = registraChave.registra(novaChavePix)
        responseObserver.onNext(
            RegistraChavePixResponse.newBuilder()
                .setPixId(chaveCriada.id.toString())
                .build()
        )
        responseObserver.onCompleted();
    }
}