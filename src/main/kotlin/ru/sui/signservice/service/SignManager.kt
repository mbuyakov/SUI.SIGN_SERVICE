package ru.sui.signservice.service

import org.apache.xml.security.signature.XMLSignature
import org.apache.xml.security.transforms.Transforms
import org.apache.xml.security.utils.JDKXPathAPI
import org.springframework.stereotype.Service
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import ru.CryptoPro.CAdES.CAdESSignature
import ru.CryptoPro.CAdES.CAdESType
import ru.CryptoPro.JCP.JCP
import ru.sui.signservice.domain.SignatureAndDigest
import ru.sui.signservice.exception.*
import sun.security.pkcs.ContentInfo
import sun.security.pkcs.PKCS7
import sun.security.pkcs.PKCS9Attribute
import sun.security.pkcs.PKCS9Attributes
import sun.security.x509.AlgorithmId
import sun.security.x509.X500Name
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.Signature
import java.security.cert.X509Certificate
import java.util.*
import javax.xml.xpath.XPathFactory
import kotlin.jvm.Throws

@Service
class SignManager(private val keyStoreHolder: KeyStoreHolder) {

    @Throws(SignServiceException::class)
    fun signPKCS7(dataToSign: ByteArray, certAlias: String): SignatureAndDigest {
        try {
            val certificate = keyStoreHolder.getX509Certificate(certAlias) ?: throw CertificateNotFoundSignServiceException(certAlias)
            val key = keyStoreHolder.getPrivateKey(certAlias) ?: throw PrivateKeyNotFoundSignServiceException(certAlias)
            return signPKCS7(dataToSign, key, certificate)
        } catch (exception: SignServiceException) {
            throw exception
        } catch (exception: Exception) {
            throw SignServiceException(exception.message, exception)
        }
    }

    @Throws(SignServiceException::class)
    fun signBES(dataToSign: ByteArray, certAlias: String): SignatureAndDigest {
        try {
            val certificateChain = keyStoreHolder.getX509CertificateChain(certAlias) ?: throw CertificateNotFoundSignServiceException(certAlias)
            val key = keyStoreHolder.getPrivateKey(certAlias) ?: throw PrivateKeyNotFoundSignServiceException(certAlias)
            return signBES(dataToSign, key, certificateChain)
        } catch (exception: SignServiceException) {
            throw exception
        } catch (exception: Exception) {
            throw SignServiceException(exception.message, exception)
        }
    }

    @Throws(SignServiceException::class)
    fun signXml(xmlToSign: Node, id: String, certAlias: String): XMLSignature {
        try {
            val list = getNodeList(xmlToSign, "//*[@Id='$id']").takeIf { it.length > 0 }
                ?: getNodeList(xmlToSign, "//*[@*[local-name()='id']='$id']").takeIf { it.length > 0 }
                ?: throw BadInputSignServiceException("Element with id '$id' not found")

            if (list.length > 1) {
                throw BadInputSignServiceException("Element with id '$id' is not unique")
            }

            val certificate = keyStoreHolder.getX509Certificate(certAlias) ?: throw CertificateNotFoundSignServiceException(certAlias)
            val key = keyStoreHolder.getPrivateKey(certAlias) ?: throw PrivateKeyNotFoundSignServiceException(certAlias)

            return signXml(list.item(0), key, certificate)
        } catch (exception: SignServiceException) {
            throw exception
        } catch (exception: Exception) {
            throw SignServiceException(exception.message, exception)
        }
    }

    private fun signPKCS7(dataToSign: ByteArray, key: PrivateKey, certificate: X509Certificate): SignatureAndDigest {
        val sigAlgorithm = keyStoreHolder.getSigAlgorithmForKey(key) ?: throw SigAlgorithmNotFoundSignServiceException(key)
        val digAlgorithm = AlgorithmId.getDigAlgFromSigAlg(sigAlgorithm)
        val encAlgorithm = AlgorithmId.getEncAlgFromSigAlg(sigAlgorithm)

        val digest = getDigest(dataToSign, digAlgorithm)

        // Данные для подписи
        val authenticatedAttributes = PKCS9Attributes(arrayOf(
            PKCS9Attribute(PKCS9Attribute.CONTENT_TYPE_OID, ContentInfo.DATA_OID), // required
            PKCS9Attribute(PKCS9Attribute.SIGNING_TIME_OID, Date()),
            PKCS9Attribute(PKCS9Attribute.MESSAGE_DIGEST_OID, digest) // required
        ))

        // Подписываем
        val signer = Signature.getInstance(sigAlgorithm)
        signer.initSign(key)
        signer.update(authenticatedAttributes.derEncoding)
        val signature = signer.sign()

        // SignerInfo
        val signerInfo = sun.security.pkcs.SignerInfo(
            X500Name(certificate.issuerDN.name),
            certificate.serialNumber,
            AlgorithmId.get(digAlgorithm),
            authenticatedAttributes,
            AlgorithmId.get(encAlgorithm),
            signature,
            null
        )

        // Собираем все вместе и пишем в стрим
        val pksc7 = PKCS7(
            arrayOf(AlgorithmId.get(digAlgorithm)),
            ContentInfo(ContentInfo.DATA_OID, null), // detached sign
            arrayOf(certificate),
            arrayOf(signerInfo)
        )

        return SignatureAndDigest(ByteArrayOutputStream().also { pksc7.encodeSignedData(it) }.toByteArray(), digest)
    }

    private fun signBES(dataToSign: ByteArray, key: PrivateKey, certificateChain: List<X509Certificate>): SignatureAndDigest {
        val sigAlgorithm = keyStoreHolder.getSigAlgorithmForKey(key) ?: throw SigAlgorithmNotFoundSignServiceException(key)
        val digAlgorithm = AlgorithmId.getDigAlgFromSigAlg(sigAlgorithm)
        val encAlgorithm = AlgorithmId.getEncAlgFromSigAlg(sigAlgorithm)

        val digest = getDigest(dataToSign, digAlgorithm)

        val cadesSignature = CAdESSignature(false)

        // Создаем подписанта CAdES-BES.
        cadesSignature.addSigner(
            JCP.PROVIDER_NAME,
            digAlgorithm,
            encAlgorithm,
            key,
            certificateChain,
            CAdESType.CAdES_BES,
            null, // Адрес TSA службы (используется только для CAdES-T или CAdES-X Long Type 1)
            false // Заверяющая подпись
        )

        val outSignatureStream = ByteArrayOutputStream()

        cadesSignature.open(outSignatureStream)
        cadesSignature.update(digest) // передаем данные для подписи
        cadesSignature.close()

        outSignatureStream.close()

        return SignatureAndDigest(outSignatureStream.toByteArray(), digest)
    }

    private fun signXml(xmlToSign: Node, key: PrivateKey, certificate: X509Certificate): XMLSignature {
        val sigUri = keyStoreHolder.getSigUriForKey(key)
            ?: throw NotFoundSignServiceException("SigUri for key algorithm '${key.algorithm}' not found")
        val digUri = keyStoreHolder.getDigUriForKey(key)
            ?: throw NotFoundSignServiceException("DigUri for key algorithm '${key.algorithm}' not found")

        val owner = xmlToSign.ownerDocument
        val refURI = xmlToSign.attributes.getNamedItem("id")?.nodeValue?.let { "#$it" }

        return XMLSignature(owner, refURI, sigUri, Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS).apply {
            this.addDocument(null, Transforms(owner), digUri)
            this.addKeyInfo(certificate)
            this.sign(key)
        }
    }

    private fun getDigest(data: ByteArray, algorithm: String): ByteArray {
        return MessageDigest.getInstance(algorithm).digest(data)
    }

    private fun getNodeList(xml: Node, xpath: String): NodeList {
        return JDKXPathAPI().selectNodeList(xml, null, xpath, xml.ownerDocument)
    }

}