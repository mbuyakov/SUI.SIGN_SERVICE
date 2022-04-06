package ru.sui.signservice.controller

import org.springframework.web.bind.annotation.*
import org.w3c.dom.Element
import ru.sui.signservice.dto.DataSignResultDto
import ru.sui.signservice.exception.CertificateNotFoundSignServiceException
import ru.sui.signservice.service.KeyStoreHolder
import ru.sui.signservice.service.SignManager
import javax.xml.bind.DatatypeConverter

@RestController
@RequestMapping("/api")
@CrossOrigin
class MainController(
    private val keyStoreHolder: KeyStoreHolder,
    private val signManager: SignManager
) {

    @GetMapping("/getAllCerts")
    fun getAllCerts(): List<String> {
        return keyStoreHolder.getKeyStore().aliases().toList()
    }

    @GetMapping("/getCert}")
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

    @PostMapping("/signXml")
    fun signXml(@RequestBody xmlToSign: Element, @RequestParam("id") id: String, @RequestParam("certAlias") certAlias: String): Element {
        return signManager.signXml(xmlToSign, id, certAlias).element
    }

}