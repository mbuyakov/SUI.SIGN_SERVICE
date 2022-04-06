package ru.sui.signservice.exception

open class SignServiceException : RuntimeException {

    constructor(message: String) : super(message)
    constructor(message: String?, cause: Throwable) : super(message, cause)

}