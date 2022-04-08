package ru.sui.signservice.controller

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.w3c.dom.Element
import ru.sui.signservice.dto.DataSignResultDto
import ru.sui.signservice.exception.CertificateNotFoundSignServiceException
import ru.sui.signservice.extension.parseString
import ru.sui.signservice.extension.transformToString
import ru.sui.signservice.service.KeyStoreHolder
import ru.sui.signservice.service.SignManager
import javax.xml.XMLConstants
import javax.xml.bind.DatatypeConverter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource

@RestController
@RequestMapping("/api")
@CrossOrigin
class MainController(
    private val keyStoreHolder: KeyStoreHolder,
    private val signManager: SignManager
) {

    private val documentBuilderFactory = DocumentBuilderFactory.newInstance().apply {
        this.isNamespaceAware = true
        this.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        this.setFeature("http://xml.org/sax/features/external-general-entities", false)
        this.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
    }

    private val transformerFactory = TransformerFactory.newInstance()

    @GetMapping("/getAllCerts")
    fun getAllCerts(): List<String> {
        return keyStoreHolder.getKeyStore().aliases().toList()
    }

    @GetMapping("/getCert")
    fun getCert(@RequestParam("certAlias") certAlias: String): String {
        return keyStoreHolder.getKeyStore().getCertificate(certAlias)
            ?.encoded
            ?.let { DatatypeConverter.printBase64Binary(it) }
            ?: throw CertificateNotFoundSignServiceException(certAlias)
    }

    @PostMapping("/signPKCS7")
    fun signPKCS7(@RequestBody dataToSign: ByteArray, @RequestParam("certAlias") certAlias: String): DataSignResultDto {
        return DataSignResultDto(signManager.signPKCS7(dataToSign, certAlias))
    }

    @PostMapping("/signBES")
    fun signBES(@RequestBody dataToSign: ByteArray, @RequestParam("certAlias") certAlias: String): DataSignResultDto {
        return DataSignResultDto(signManager.signBES(dataToSign, certAlias))
    }

    @PostMapping(
        path = ["/signXml"],
        consumes = [MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE],
        produces = [MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE]
    )
    fun signXml(@RequestBody xmlToSign: String, @RequestParam("id") id: String, @RequestParam("certAlias") certAlias: String): String {
        val documentToSign = documentBuilderFactory.newDocumentBuilder().parseString(xmlToSign).documentElement
        val signResult = signManager.signXml(documentToSign, id, certAlias)
        return createTransformer().transformToString(DOMSource(signResult.element))
    }

    private fun createTransformer(): Transformer {
        return transformerFactory.newTransformer().apply {
            this.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
            this.setOutputProperty(OutputKeys.METHOD, "xml")
            this.setOutputProperty(OutputKeys.INDENT, "no")
            this.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
        }
    }

}