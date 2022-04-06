package ru.sui.signservice.exception

open class NotFoundSignServiceException : SignServiceException {

    constructor(message: String) : super(message)
    constructor(message: String?, cause: Throwable) : super(message, cause)

}