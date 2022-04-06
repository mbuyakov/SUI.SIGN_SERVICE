package ru.sui.signservice.exception

class BadInputSignServiceException : SignServiceException {

    constructor(message: String) : super(message)
    constructor(message: String?, cause: Throwable) : super(message, cause)

}