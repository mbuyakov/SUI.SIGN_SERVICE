package ru.sui.signservice.configuration

import org.apache.xml.security.Init
import org.springframework.context.annotation.Configuration
import javax.annotation.PostConstruct

@Configuration
class XmlSecurityConfiguration {

    @PostConstruct
    fun postConstruct() {
        // Fix for SMEV3
        System.setProperty("org.apache.xml.security.ignoreLineBreaks", "true")

        // Xml security init
        try {
            Class.forName("ru.CryptoPro.JCPxml.XmlInit").getMethod("init").invoke(null)
        } catch (exception: Throwable) {
            Init.init()
        }
    }

}