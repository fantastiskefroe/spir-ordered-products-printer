package dk.fantastiskefroe.spir.orderedproductsprinter;

import dk.fantastiskefroe.spir.orderedproductsprinter.config.ApplicationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(ApplicationProperties.class)
public class OrderedProductsPrinterApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderedProductsPrinterApplication.class, args);
    }

}
