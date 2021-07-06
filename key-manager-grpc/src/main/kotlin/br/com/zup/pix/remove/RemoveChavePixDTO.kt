package br.com.zup.pix.remove

import br.com.zup.pix.ValidUUID
import io.micronaut.core.annotation.Introspected

@Introspected
data class RemoveChavePixDTO(
    @field:ValidUUID val clienteId: String,
    @field:ValidUUID val pixId: String
)