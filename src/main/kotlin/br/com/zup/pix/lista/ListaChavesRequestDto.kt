package br.com.zup.pix.lista

import br.com.zup.shared.validation.ValidUUID
import io.micronaut.core.annotation.Introspected
import javax.validation.constraints.NotBlank

@Introspected
data class ListaChavesRequestDto(
    @field:NotBlank @field:ValidUUID val clienteId: String
)
