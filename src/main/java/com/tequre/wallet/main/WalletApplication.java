package com.tequre.wallet.main;

import org.springframework.boot.SpringApplication;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableScheduling;


import javax.annotation.PostConstruct;

import java.util.Properties;
import java.util.TimeZone;

@SpringBootApplication
@ComponentScan(value = "com.tequre.wallet")
@EnableAutoConfiguration
@Configuration
@EnableReactiveMongoRepositories(value = "com.tequre.wallet")
@EnableMongoRepositories(value = "com.tequre.wallet")
@EnableMongoAuditing
@EnableScheduling
public class WalletApplication {

    public static void main(String[] args) {
        SpringApplication.run(WalletApplication.class, args);
    }

    @PostConstruct
    void started() {
        TimeZone.setDefault(TimeZone.getTimeZone("IST"));
    }
    
    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("smtp.office365.com");
        mailSender.setPort(587);
        mailSender.setUsername("application_alert@gen-xt.com");
        mailSender.setPassword("@!ert@pp!i(@t!0n");
        Properties properties=new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        
        mailSender.setJavaMailProperties(properties);
        return mailSender;
    }
    
}
