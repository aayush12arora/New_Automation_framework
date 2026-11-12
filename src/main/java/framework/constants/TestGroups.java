package framework.constants;

/**
 * TestNG group names. Every test is in {@link #REGRESSION}; the 40 business-critical
 * checks are also in {@link #SMOKE}. Run one group with {@code mvn test -Dgroups=smoke}.
 */
public final class TestGroups {

    private TestGroups() {
        // Prevent instantiation.
    }

    public static final String SMOKE = "smoke";
    public static final String REGRESSION = "regression";
}
