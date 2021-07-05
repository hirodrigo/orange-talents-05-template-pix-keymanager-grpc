package br.com.zup.pix.registra

import br.com.zup.KeyRequest
import br.com.zup.TipoChave.*
import br.com.zup.TipoConta.*

fun KeyRequest.paraNovaChavePix(): NovaChavePix {
    return NovaChavePix(
        clienteId = this.clienteId,
        tipoChave = when (this.tipoChave) {
            UNKNOWN_TIPO_CHAVE -> null
            else -> TipoChave.valueOf(this.tipoChave.name)
        },
        chave = this.chave,
        tipoConta = when (this.tipoConta) {
            UNKNOWN_TIPO_CONTA -> null
            else -> TipoConta.valueOf(this.tipoConta.name)
        }
    )
}