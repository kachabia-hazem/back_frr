package com.hazem.worklink.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            // Sérialise LocalDate en "2023-01-15" (pas en tableau [2023,1,15])
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            // Deserializer flexible pour LocalDate : gère "", null, "YYYY", "YYYY-MM-DD"
            builder.deserializerByType(LocalDate.class, new FlexibleLocalDateDeserializer());
        };
    }

    /**
     * Deserializer robuste pour LocalDate.
     * Accepte :
     *  - null / "" / "null" / "N/A" / "present" / "présent" → null
     *  - "YYYY-MM-DD" → LocalDate normal
     *  - "YYYY-MM"    → premier jour du mois
     *  - "YYYY"       → 1er janvier de l'année
     */
    static class FlexibleLocalDateDeserializer extends StdDeserializer<LocalDate> {

        FlexibleLocalDateDeserializer() {
            super(LocalDate.class);
        }

        @Override
        public LocalDate deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            String text = p.getText();
            if (text == null) return null;
            text = text.trim();

            if (text.isEmpty()
                    || text.equalsIgnoreCase("null")
                    || text.equalsIgnoreCase("N/A")
                    || text.equalsIgnoreCase("unknown")
                    || text.equalsIgnoreCase("present")
                    || text.equalsIgnoreCase("présent")
                    || text.equalsIgnoreCase("en cours")
                    || text.equalsIgnoreCase("aujourd'hui")) {
                return null;
            }

            // Format YYYY-MM-DD
            try {
                return LocalDate.parse(text);
            } catch (DateTimeParseException ignored) {}

            // Format YYYY-MM
            try {
                String[] parts = text.split("[-/]");
                if (parts.length == 2) {
                    int year  = Integer.parseInt(parts[0].trim());
                    int month = Integer.parseInt(parts[1].trim());
                    if (year > 1900 && year < 2100 && month >= 1 && month <= 12) {
                        return LocalDate.of(year, month, 1);
                    }
                }
            } catch (Exception ignored) {}

            // Format YYYY (année seule)
            try {
                int year = Integer.parseInt(text);
                if (year > 1900 && year < 2100) {
                    return LocalDate.of(year, 1, 1);
                }
            } catch (NumberFormatException ignored) {}

            // Format non reconnu → null (évite le 400)
            return null;
        }
    }
}
