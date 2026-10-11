package com.pharmacy.pharmacy_backend.config;

import com.mongodb.ConnectionString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.mongo.MongoConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class MongoConfig {

    private static final Logger log = LoggerFactory.getLogger(MongoConfig.class);

    /**
     * Sanitizes raw MongoDB URI strings entered in hosting dashboards (e.g. Render, Railway, Heroku)
     * where users frequently include quotes, prefix names, or whitespace.
     */
    public static String cleanConnectionString(String raw) {
        if (raw == null) return "";
        String s = raw.strip();

        // Strip leading common shell / key assignment prefixes
        String[] prefixes = {"export MONGODB_URI=", "MONGODB_URI=", "MONGODB_URI:", "MONGODB_URI :", "MONGODB_URI ="};
        for (String p : prefixes) {
            if (s.startsWith(p)) {
                s = s.substring(p.length()).strip();
                break;
            }
        }

        // Strip enclosing single or double quotes
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            if (s.length() >= 2) {
                s = s.substring(1, s.length() - 1).strip();
            }
        }

        return s;
    }

    @Bean
    @ConditionalOnExpression("!'${spring.data.mongodb.uri:}'.trim().isEmpty()")
    public MongoConnectionDetails mongoConnectionDetails(Environment environment) {
        String raw = environment.getProperty("spring.data.mongodb.uri");
        String clean = cleanConnectionString(raw);

        if (clean.isBlank()) {
            throw new IllegalArgumentException(
                "MONGODB_URI is blank. Please configure a valid MongoDB Atlas connection string starting with mongodb:// or mongodb+srv://"
            );
        }

        log.info("Initialized MongoDB connection with protocol: {}",
            clean.startsWith("mongodb+srv://") ? "mongodb+srv" : (clean.startsWith("mongodb://") ? "mongodb" : "custom"));

        final ConnectionString connectionString = new ConnectionString(clean);
        return () -> connectionString;
    }
}
