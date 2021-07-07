package br.com.zup.shared.grpc.handlers

import br.com.zup.shared.grpc.ExceptionHandler
import br.com.zup.shared.grpc.ExceptionHandler.*
import io.grpc.Status
import javax.inject.Singleton
import javax.validation.ConstraintViolationException

@Singleton
class ConstraintViolationExceptionHandler : ExceptionHandler<ConstraintViolationException> {

    override fun handle(e: ConstraintViolationException): ExceptionHandler.StatusWithDetails {
        return StatusWithDetails(Status.INVALID_ARGUMENT.withDescription(e.message))
    }

    override fun supports(e: Exception): Boolean {
        return e is ConstraintViolationException
    }
}