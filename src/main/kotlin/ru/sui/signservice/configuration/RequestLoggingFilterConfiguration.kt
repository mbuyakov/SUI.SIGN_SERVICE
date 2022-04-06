package ru.sui.signservice.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.filter.CommonsRequestLoggingFilter

@Configuration
class RequestLoggingFilterConfiguration {

    @Bean
    fun logFilter(): CommonsRequestLoggingFilter {
        return CommonsRequestLoggingFilter().apply {
            this.setIncludeQueryString(true)
            this.setIncludePayload(true)
            this.setMaxPayloadLength(10000)
            this.setIncludeHeaders(false)
            this.setAfterMessagePrefix("REQUEST DATA : ")
        }
    }

}