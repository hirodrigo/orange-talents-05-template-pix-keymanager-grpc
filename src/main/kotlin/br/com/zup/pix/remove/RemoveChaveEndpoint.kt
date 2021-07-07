package br.com.zup.pix.remove

import br.com.zup.KeyManagerRemoveServiceGrpc
import br.com.zup.RemoveChavePixRequest
import br.com.zup.RemoveChavePixResponse
import br.com.zup.shared.grpc.ErrorHandler
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import javax.inject.Singleton

@ErrorHandler
@Singleton
class RemoveChaveEndpoint(
    @Inject val removeChave: RemoveChave
) : KeyManagerRemoveServiceGrpc.KeyManagerRemoveServiceImplBase() {

    override fun remove(request: RemoveChavePixRequest, responseObserver: StreamObserver<RemoveChavePixResponse>) {
        val dto = request.paraRemoveChavePixDTO()
        val chaveRemovida = removeChave.remove(dto)
        responseObserver.onNext(
            RemoveChavePixResponse.newBuilder()
                .setPixId(chaveRemovida.id.toString())
                .build()
        )
        responseObserver.onCompleted();
    }
}