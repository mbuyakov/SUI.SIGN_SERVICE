package ru.sui.signservice.exception

class CertificateNotFoundSignServiceException(alias: String) : NotFoundSignServiceException("Certificate '${alias}' not found in a store")