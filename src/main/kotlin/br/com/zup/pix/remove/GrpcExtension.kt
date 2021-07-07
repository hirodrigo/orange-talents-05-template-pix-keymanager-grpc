package br.com.zup.pix.remove

import br.com.zup.RemoveChavePixRequest

fun RemoveChavePixRequest.paraRemoveChavePixDTO(): RemoveChavePixDTO {
    return RemoveChavePixDTO(
        clienteId = this.clienteId,
        pixId = this.pixId
    )
}