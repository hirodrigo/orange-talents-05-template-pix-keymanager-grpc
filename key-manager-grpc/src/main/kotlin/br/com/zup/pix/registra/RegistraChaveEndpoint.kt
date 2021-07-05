package br.com.zup.pix.registra

import br.com.zup.KeyRequest
import br.com.zup.KeyResponse
import br.com.zup.KeyServiceGrpc
import br.com.zup.shared.grpc.ErrorHandler
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import javax.inject.Singleton

@ErrorHandler
@Singleton
class RegistraChaveEndpoint(
    @Inject val registraChave: RegistraChave
) : KeyServiceGrpc.KeyServiceImplBase() {

    override fun cadastrar(request: KeyRequest, responseObserver: StreamObserver<KeyResponse>) {
        val novaChavePix = request.paraNovaChavePix()
        val chaveCriada = registraChave.registra(novaChavePix)
        responseObserver.onNext(
            KeyResponse.newBuilder()
                .setPixId(chaveCriada.id.toString())
                .build()
        )
        responseObserver.onCompleted();
    }
}