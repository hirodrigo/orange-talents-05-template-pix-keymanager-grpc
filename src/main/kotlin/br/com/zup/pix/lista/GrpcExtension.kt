package br.com.zup.pix.lista

import br.com.zup.ListaChavesPixRequest
import io.micronaut.validation.validator.Validator
import javax.validation.ConstraintViolationException

fun ListaChavesPixRequest.paraDto(validator: Validator): ListaChavesRequestDto {
    val request = ListaChavesRequestDto(this.clienteId)
    val violations = validator.validate(request)
    if (violations.isNotEmpty()) {
        throw ConstraintViolationException(violations)
    }
    return request
}