package com.alan.work;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@Slf4j
public class LiteratureCrawlerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LiteratureCrawlerApplication.class, args);
        log.info("AI辅助文献爬虫系统启动成功！");
        log.info("访问地址: http://localhost:8080");
        log.info("H2控制台: http://localhost:8080/h2-console");
    }
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}