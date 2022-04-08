package ru.sui.signservice.extension

import java.io.StringWriter
import javax.xml.transform.Source
import javax.xml.transform.Transformer
import javax.xml.transform.stream.StreamResult

fun Transformer.transformToString(source: Source): String {
    val stringWriter = StringWriter()
    stringWriter.use { this.transform(source, StreamResult(it)) }
    return stringWriter.toString()
}