package com.iroute.ibatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class IbatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(IbatchApplication.class, args);
    }
}
