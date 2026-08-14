package com.smartdoc;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@MapperScan({"com.smartdoc.**.mapper", "com.smartdoc.ai.provider", "com.smartdoc.ai.vision"})
@SpringBootApplication
public class SmartDocApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartDocApplication.class, args);
    }
}
