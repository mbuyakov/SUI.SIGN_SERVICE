package ru.sui.signservice.dto

import ru.sui.signservice.domain.SignatureAndDigest

class DataSignResultDto(
    val signature: ByteArray,
    val digest: ByteArray
) {

    constructor(signatureAndDigest: SignatureAndDigest) : this(signatureAndDigest.signature, signatureAndDigest.digest)

}