package es.activa.config;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.Map;

public final class PersistenceConfig {
    private PersistenceConfig() {
    }

    public static EntityManagerFactory createEntityManagerFactory() {
        Map<String, Object> properties = new HashMap<>();
        putEnvironmentValue(properties, "DB_URL", "jakarta.persistence.jdbc.url");
        putEnvironmentValue(properties, "DB_USER", "jakarta.persistence.jdbc.user");
        putEnvironmentValue(properties, "DB_PASSWORD", "jakarta.persistence.jdbc.password");
        return Persistence.createEntityManagerFactory("activaPU", properties);
    }

    private static void putEnvironmentValue(Map<String, Object> properties, String variable, String property) {
        String value = System.getenv(variable);
        if (value != null) {
            properties.put(property, value);
        }
    }
}
