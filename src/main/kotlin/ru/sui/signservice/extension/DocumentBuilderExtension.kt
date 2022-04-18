package ru.sui.signservice.extension

import org.w3c.dom.Document
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.io.StringReader
import javax.xml.parsers.DocumentBuilder

fun DocumentBuilder.parseString(data: String): Document {
    return StringReader(data).use { this.parse(InputSource(it)) }
}

fun DocumentBuilder.parse(data: ByteArray): Document {
    return ByteArrayInputStream(data).use { this.parse(InputSource(it)) }
}