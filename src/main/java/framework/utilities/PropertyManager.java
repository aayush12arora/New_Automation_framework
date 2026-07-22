package framework.utilities;

import framework.constants.FrameworkConstants;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads and exposes values from {@code framework.properties}.
 * The file is read once from the classpath and cached for the JVM lifetime.
 */
public final class PropertyManager {

    private static final Logger LOG = LoggerUtil.getLogger(PropertyManager.class);
    private static final Properties PROPERTIES = load();

    private PropertyManager() {
        // Prevent instantiation.
    }

    private static Properties load() {
        Properties properties = new Properties();
        try (InputStream input = PropertyManager.class.getClassLoader()
                .getResourceAsStream(FrameworkConstants.PROPERTIES_FILE)) {
            if (input == null) {
                throw new IllegalStateException(
                        "Configuration file not found on classpath: " + FrameworkConstants.PROPERTIES_FILE);
            }
            properties.load(input);
            LOG.info("Loaded configuration from {}", FrameworkConstants.PROPERTIES_FILE);
            return properties;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + FrameworkConstants.PROPERTIES_FILE, e);
        }
    }

    /**
     * Returns a configuration value. A matching JVM system property ({@code -Dkey=value})
     * takes precedence over the file, so CI can override any setting without editing
     * {@code framework.properties} (e.g. {@code mvn test -Dheadless=true -Dbrowser=chrome}).
     */
    public static String get(String key) {
        String override = System.getProperty(key);
        if (override != null && !override.isBlank()) {
            return override.trim();
        }
        String value = PROPERTIES.getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing property: " + key);
        }
        return value.trim();
    }

    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(get(key));
    }

    public static int getInt(String key) {
        return Integer.parseInt(get(key));
    }
}
