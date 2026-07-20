package framework.utilities;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;

/**
 * Jackson-backed helpers for reading JSON test data into POJOs.
 */
public final class JsonUtils {

    private static final Logger LOG = LoggerUtil.getLogger(JsonUtils.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonUtils() {
        // Prevent instantiation.
    }

    /**
     * Reads a JSON file from the classpath and maps it to the given type.
     *
     * @param classpathResource resource path, e.g. {@code "testdata/login.json"}
     * @param type              target POJO class
     */
    public static <T> T read(String classpathResource, Class<T> type) {
        try (InputStream input = JsonUtils.class.getClassLoader().getResourceAsStream(classpathResource)) {
            if (input == null) {
                throw new IllegalArgumentException("JSON resource not found: " + classpathResource);
            }
            return MAPPER.readValue(input, type);
        } catch (IOException e) {
            LOG.error("Failed to parse JSON resource '{}'", classpathResource, e);
            throw new IllegalStateException("Unable to read JSON: " + classpathResource, e);
        }
    }

    public static <T> T fromString(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to parse JSON string", e);
        }
    }

    /** @return {@code true} if the classpath resource exists. */
    public static boolean resourceExists(String classpathResource) {
        return JsonUtils.class.getClassLoader().getResource(classpathResource) != null;
    }
}
