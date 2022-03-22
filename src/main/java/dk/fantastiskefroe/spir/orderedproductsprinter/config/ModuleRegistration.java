package dk.fantastiskefroe.spir.orderedproductsprinter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModuleRegistration {

    @Autowired
    void configureObjectMapper(final ObjectMapper mapper) {
        mapper.registerModule(new JsonNullableModule());
    }
}
