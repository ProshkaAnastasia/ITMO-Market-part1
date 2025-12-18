package ru.itmo.user.config

import feign.codec.Decoder
import feign.codec.Encoder
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.http.HttpMessageConverters
import org.springframework.cloud.openfeign.support.SpringDecoder
import org.springframework.cloud.openfeign.support.SpringEncoder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import com.fasterxml.jackson.databind.ObjectMapper

@Configuration
class FeignConfig {

    @Bean
    @ConditionalOnMissingBean
    fun httpMessageConverters(objectMapper: ObjectMapper): HttpMessageConverters {
        val converter = MappingJackson2HttpMessageConverter(objectMapper)
        return HttpMessageConverters(converter)
    }

    @Bean
    fun feignDecoder(httpMessageConverters: HttpMessageConverters): Decoder {
        return SpringDecoder { httpMessageConverters }
    }

    @Bean
    fun feignEncoder(httpMessageConverters: HttpMessageConverters): Encoder {
        return SpringEncoder { httpMessageConverters }
    }
}
