package com.occasi.application.config

import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.filter.ShallowEtagHeaderFilter

@Configuration
class ETagConfig {

    @Bean
    fun shallowEtagHeaderFilter(): FilterRegistrationBean<ShallowEtagHeaderFilter> {
        val filter = FilterRegistrationBean(ShallowEtagHeaderFilter())
        filter.addUrlPatterns("/henna-artists/*", "/henna-designs/*", "/invitation-cards/*")
        filter.setName("etagFilter")
        return filter
    }
}
