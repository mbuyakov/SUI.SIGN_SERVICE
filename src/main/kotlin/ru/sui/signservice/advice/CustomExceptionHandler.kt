package ru.sui.signservice.advice

import mu.KotlinLogging
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import org.springframework.web.util.WebUtils
import ru.sui.signservice.dto.ErrorDto
import ru.sui.signservice.exception.BadInputSignServiceException
import ru.sui.signservice.exception.NotFoundSignServiceException
import ru.sui.signservice.exception.SignServiceException

private val log = KotlinLogging.logger { }

@ControllerAdvice
class CustomExceptionHandler : ResponseEntityExceptionHandler() {

    @ExceptionHandler(SignServiceException::class)
    fun handleAll(exception: SignServiceException, request: WebRequest): ResponseEntity<Any?>? {
        val status = when (exception) {
            is BadInputSignServiceException -> HttpStatus.BAD_REQUEST
            is NotFoundSignServiceException -> HttpStatus.NOT_FOUND
            else -> HttpStatus.INTERNAL_SERVER_ERROR
        }

        return this.handleExceptionInternal(exception, null, HttpHeaders(), status, request)
    }

    override fun handleExceptionInternal(
        exception: Exception,
        body: Any?,
        headers: HttpHeaders,
        status: HttpStatus,
        request: WebRequest
    ): ResponseEntity<Any?> {
        log.debug(exception) { "Exception while processing request" }

        if (HttpStatus.INTERNAL_SERVER_ERROR == status) {
            request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, exception, WebRequest.SCOPE_REQUEST)
        }

        val details = exception.stackTraceToString()
        val message = exception.message ?: details.lineSequence().first()

        return ResponseEntity(ErrorDto(message, details), headers, status)
    }

}