package ru.sui.signservice.service

import org.springframework.core.env.Environment
import org.springframework.core.env.get
import org.springframework.stereotype.Service
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.concurrent.ConcurrentHashMap

@Suppress("UNCHECKED_CAST")
@Service
class KeyStoreHolder(private val environment: Environment) {

    private val keyStore = KeyStore.getInstance("HDImageStore").also { it.load(null, null) }

    private val privateKeyCache = ConcurrentHashMap<String, PrivateKey>()
    private val x509CertificateCache = ConcurrentHashMap<String, X509Certificate>()
    private val x509CertificateChainCache = ConcurrentHashMap<String, List<X509Certificate>>()

    fun getPrivateKey(alias: String): PrivateKey? {
        if (privateKeyCache.containsKey(alias)) {
            return privateKeyCache[alias]
        }

        val password = environment["$alias.password"] ?: environment["CRYPTOPRO_${alias}_PASSWORD"]
        val privateKey = keyStore.getKey(alias, password?.toCharArray()) as PrivateKey?

        if (privateKey != null) {
            privateKeyCache[alias] = privateKey
        }

        return privateKey
    }

    fun getX509Certificate(alias: String): X509Certificate? {
        if (x509CertificateCache.containsKey(alias)) {
            return x509CertificateCache[alias]
        }

        val certificate = keyStore.getCertificate(alias) as X509Certificate?

        if (certificate != null) {
            x509CertificateCache[alias] = certificate
        }

        return certificate
    }

    fun getX509CertificateChain(alias: String): List<X509Certificate>? {
        if (x509CertificateChainCache.containsKey(alias)) {
            return x509CertificateChainCache[alias]
        }

        val certificateChain = keyStore.getCertificateChain(alias)?.map { it as X509Certificate }?.toTypedArray()

        if (certificateChain != null && certificateChain.isNotEmpty()) {
            x509CertificateChainCache[alias] = certificateChain.toList()
        }

        return certificateChain?.toList()
    }

    fun getSigAlgorithmForKey(key: PrivateKey): String? {
        return environment.getProperty("sigAlgFromKeyAlg.${key.algorithm}")
    }

    fun getSigUriForKey(key: PrivateKey): String? {
        return environment.getProperty("sigUriFromKeyAlg.${key.algorithm}")
    }

    fun getDigUriForKey(key: PrivateKey): String? {
        return getSigUriForKey(key)?.let { environment.getProperty("digUriFromSigUri.$it") }
    }

    fun getKeyStore(): KeyStore {
        return keyStore
    }

}