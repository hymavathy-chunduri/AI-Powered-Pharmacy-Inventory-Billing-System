package com.pharmacy.pharmacy_backend.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MongoConfigTest {

    @Test
    void testCleanConnectionStringVariations() {
        String expected = "mongodb+srv://user:pass@cluster.mongodb.net/test?retryWrites=true&w=majority";

        // Plain string
        assertEquals(expected, MongoConfig.cleanConnectionString(expected));

        // Enclosed in double quotes
        assertEquals(expected, MongoConfig.cleanConnectionString("\"" + expected + "\""));

        // Enclosed in single quotes
        assertEquals(expected, MongoConfig.cleanConnectionString("'" + expected + "'"));

        // With leading/trailing spaces
        assertEquals(expected, MongoConfig.cleanConnectionString("   " + expected + "  \n\t"));

        // With MONGODB_URI= prefix
        assertEquals(expected, MongoConfig.cleanConnectionString("MONGODB_URI=" + expected));

        // With MONGODB_URI= and quotes
        assertEquals(expected, MongoConfig.cleanConnectionString("MONGODB_URI=\"" + expected + "\""));

        // With export MONGODB_URI=
        assertEquals(expected, MongoConfig.cleanConnectionString("export MONGODB_URI=" + expected));

        // Null and blank handling
        assertEquals("", MongoConfig.cleanConnectionString(null));
        assertEquals("", MongoConfig.cleanConnectionString("   "));
    }
}
