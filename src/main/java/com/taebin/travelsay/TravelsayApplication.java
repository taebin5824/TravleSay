package com.taebin.travelsay;

import com.taebin.travelsay.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@ConfigurationPropertiesScan
@SpringBootApplication
public class TravelsayApplication {

    public static void main(String[] args) {
        SpringApplication.run(TravelsayApplication.class, args);
    }

}
