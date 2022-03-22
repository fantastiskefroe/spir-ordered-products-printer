package dk.fantastiskefroe.spir.orderedproductsprinter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "app", ignoreUnknownFields = false)
public record ApplicationProperties(String printSchedule, OrderApiProperties orderApi, PrintServerProperties printServer) {
    public record OrderApiProperties(String baseUri) {}
    public record PrintServerProperties(String host, String username, String privateKey, String printerPath) {}
}
