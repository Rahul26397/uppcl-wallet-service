package com.tequre.wallet.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Collections;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Bean
    public Docket apiDocket() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.tequre.wallet"))
                .paths(PathSelectors.any())
                .build()
                .apiInfo(getApiInfo());
    }

    private ApiInfo getApiInfo() {
        return new ApiInfo(
                "Blockchain Wallet",
                "Payment Wallet using Blockchain Technology",
                "1.0.0",
                "Terms of Service",
                new Contact("Tequre", "www.tequre.com", "info@tequre.com"),
                "",
                "",
                Collections.emptyList());
    }

    /* private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Json.class, new SpringfoxJsonToGsonAdapter())
            .create();

    private static class SpringfoxJsonToGsonAdapter implements JsonSerializer<Json> {

        @Override
        public JsonElement serialize(Json json, Type type, JsonSerializationContext jsonSerializationContext) {
            final JsonParser parser = new JsonParser();
            return parser.parse(json.value());
        }
    } */
}