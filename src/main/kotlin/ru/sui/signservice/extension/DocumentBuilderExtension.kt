package ru.sui.signservice.extension

import org.w3c.dom.Document
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilder

fun DocumentBuilder.parseString(data: String): Document {
    return StringReader(data).use { this.parse(InputSource(it)) }
}