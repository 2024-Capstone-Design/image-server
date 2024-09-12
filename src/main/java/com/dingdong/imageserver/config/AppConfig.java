package com.dingdong.imageserver.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@Getter
@Setter
public class AppConfig {

    @Value("${proxy.midjourney.url}")
    private String mjUrl;

    @Value("${proxy.bg-remove.url}")
    private String bgRemoveUrl;

    private String characterContentPrompt;
    private String characterStylePrompt;
    private String characterPramsPrompt;

    private String backgroundContentPrompt;
    private String backgroundStylePrompt;
    private String backgroundPramsPrompt;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}

