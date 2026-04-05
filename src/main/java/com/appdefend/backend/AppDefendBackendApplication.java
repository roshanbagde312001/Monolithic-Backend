package com.appdefend.backend;

import com.appdefend.backend.config.JwtProperties;
import com.appdefend.backend.config.LicenseProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, LicenseProperties.class})
public class AppDefendBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppDefendBackendApplication.class, args);
    }
}
