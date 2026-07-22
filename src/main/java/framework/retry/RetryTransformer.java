package framework.retry;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Attaches {@link RetryAnalyzer} to every {@code @Test} method automatically,
 * so test classes never declare {@code retryAnalyzer} themselves — retry
 * behaviour is controlled purely through {@code framework.properties}.
 *
 * <p><b>Registration:</b> this is wired in via
 * {@code META-INF/services/org.testng.ITestNGListener} (ServiceLoader), <em>not</em>
 * {@code @Listeners}. An {@link IAnnotationTransformer} must be active before TestNG
 * reads {@code @Test} annotations; a transformer registered through {@code @Listeners}
 * on a test class loads too late in that phase, so the analyzer would never be attached
 * and failing tests would silently not retry.</p>
 */
public class RetryTransformer implements IAnnotationTransformer {

    @Override
    public void transform(ITestAnnotation annotation, Class testClass, Constructor testConstructor, Method testMethod) {
        annotation.setRetryAnalyzer(RetryAnalyzer.class);
    }
}
