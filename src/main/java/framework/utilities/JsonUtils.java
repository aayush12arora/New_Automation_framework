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

    /** Maps an arbitrary object (e.g. a {@code Map} from Excel) onto a POJO. */
    public static <T> T convert(Object from, Class<T> type) {
        return MAPPER.convertValue(from, type);
    }

    /** @return {@code true} if the classpath resource exists. */
    public static boolean resourceExists(String classpathResource) {
        return JsonUtils.class.getClassLoader().getResource(classpathResource) != null;
    }

    /**
     * Pretty-prints {@code value} as JSON for display (e.g. in a report code block).
     * Accepts either a raw JSON string (parsed, then re-printed) or a POJO (serialized directly).
     * Falls back to {@code value}'s own {@code toString()} if it isn't valid/serializable JSON,
     * and returns {@code ""} for {@code null}.
     */
    public static String prettyPrint(Object value) {
        if (value == null) {
            return "";
        }
        try {
            if (value instanceof String json) {
                if (json.isBlank()) {
                    return "";
                }
                return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(MAPPER.readTree(json));
            }
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (IOException e) {
            return String.valueOf(value);
        }
    }
}
