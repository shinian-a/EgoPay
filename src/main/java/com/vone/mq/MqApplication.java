package com.vone.mq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MqApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(MqApplication.class, args);
    }

    /**
     * war 包部署到外部 Tomcat 时的入口；内置方式运行不受影响。
     */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(MqApplication.class);
    }
}
