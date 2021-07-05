package br.com.zup.pix.registra

import io.micronaut.core.annotation.Introspected
import java.util.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@ValidPixKey
@Introspected
data class NovaChavePix(
    @field:NotBlank @field:ValidUUID val clienteId: String,
    @field:NotNull val tipoChave: TipoChave?,
    @field:Size(max = 77) val chave: String,
    @field:NotNull val tipoConta: TipoConta?
) {
    fun toModel(conta: ContaAssociada): ChavePix {
        return ChavePix(
            clienteId = UUID.fromString(this.clienteId),
            tipoChave = this.tipoChave!!,
            chave =
            if (this.tipoChave == TipoChave.ALEATORIA)
                UUID.randomUUID().toString()
            else
                this.chave,
            tipoConta = this.tipoConta!!,
            conta = conta
        )
    }
}