package br.com.zup.pix.consulta

import br.com.zup.ConsultaChavePixRequest
import br.com.zup.ConsultaChavePixRequest.FiltroCase.*
import io.micronaut.validation.validator.Validator
import javax.validation.ConstraintViolationException

fun ConsultaChavePixRequest.paraFiltro(validator: Validator): Filtro {

    val filtro = when (filtroCase!!) {
        PIXID -> pixId.let {
            Filtro.PorPixId(clientId = it.clienteId, pixId = it.pixId)
        }
        CHAVE -> Filtro.PorChave(chave)
        FILTRO_NOT_SET -> Filtro.Invalido()
    }

    val violations = validator.validate(filtro)
    if (violations.isNotEmpty()) {
        throw ConstraintViolationException(violations)
    }

    return filtro
}