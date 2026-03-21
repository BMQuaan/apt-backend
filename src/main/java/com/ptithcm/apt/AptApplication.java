package com.ptithcm.apt;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(
    info = @Info(
        title = "APT",
        version = "1.0.0",
        description = "Tài liệu mô tả các API cho hệ thống Backend"
    )
)
public class AptApplication {

    public static void main(String[] args) {
        SpringApplication.run(AptApplication.class, args);
    }

}
