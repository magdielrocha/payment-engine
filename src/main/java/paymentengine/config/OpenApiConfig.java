package paymentengine.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Payment Engine - API")
                        .version("1.0.0")
                        .description("A RESTful API designed for processing financial transfers with high concurrency and strong data integrity.")
                        .contact( new Contact()
                                .name("Magdiel Rocha")
                                .email("magdielrocha.dev@gmail.com")));
    }

}
