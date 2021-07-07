package br.com.zup.integracao.bcb

import br.com.zup.pix.registra.ContaAssociada
import java.time.LocalDateTime

data class DeletePixKeyRequest(
    val key: String
){
    val participant: String = ContaAssociada.ITAU_UNIBANCO_ISBP
}

data class DeletePixKeyResponse(
    val key: String,
    val participant: String,
    val deletedAt: LocalDateTime
)



