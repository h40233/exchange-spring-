package com.exchange.exchange.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

// ====== 檔案總結 ======
// AppConfig 負責 Spring 的全域 Bean 設定。
@Configuration
public class AppConfig {
    
    // 定義 RestTemplate Bean
    // 用於在 Service 或 Controller 中發送 HTTP 請求 (例如呼叫 Binance API)
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}