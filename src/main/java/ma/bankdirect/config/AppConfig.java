package ma.bankdirect.config;

import java.io.InputStream;
import java.util.Properties;

public final class AppConfig {
    private static final Properties PROPERTIES = new Properties();

    private AppConfig() {}

    static {
        try (
            InputStream in = AppConfig.class.getResourceAsStream("/app.properties")) {

            if (in == null) {
                throw new IllegalStateException("Fichier app.properties introuvable dans le classpath");
            }
            PROPERTIES.load(in);
        } catch (Exception e) {
            throw new ExceptionInInitializerError("Impossible d'initialiser AppConfig: " + e.getMessage());
        }
    }

    public static String get(String key) {
        return PROPERTIES.getProperty(key);
    }

    public static String getOrDefault(String key, String defaultValue) {
        return PROPERTIES.getProperty(key, defaultValue);
    }
}
