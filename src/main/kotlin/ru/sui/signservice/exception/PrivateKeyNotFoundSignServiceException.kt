package ru.sui.signservice.exception

class PrivateKeyNotFoundSignServiceException(alias: String) : NotFoundSignServiceException("Private key for certificate '${alias}' not found in a store")