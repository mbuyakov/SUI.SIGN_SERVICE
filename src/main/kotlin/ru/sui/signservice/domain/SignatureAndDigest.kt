package ru.sui.signservice.domain

class SignatureAndDigest(
    val signature: ByteArray,
    val digest: ByteArray
)