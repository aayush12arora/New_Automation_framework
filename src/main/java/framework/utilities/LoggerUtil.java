package framework.utilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thin factory around SLF4J so callers get a class-scoped logger
 * without repeating LoggerFactory boilerplate.
 */
public final class LoggerUtil {

    private LoggerUtil() {
        // Prevent instantiation.
    }

    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }
}
