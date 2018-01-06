package software.wings.metrics.appdynamics;

import static java.util.Arrays.asList;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by mike@ on 6/14/17.
 */
public class AppdynamicsConstants {
  public static final String RESPONSE_TIME_95 = "95th Percentile Response Time (ms)";
  public static final String ERRORS_PER_MINUTE = "Errors per Minute";
  public static final String STALL_COUNT = "Stall Count";
  public static final String NUMBER_OF_SLOW_CALLS = "Number of Slow Calls";

  public static Map<String, String> METRIC_NAMES_TO_VARIABLES = new HashMap<>();
  public static Map<String, String> VARIABLES_TO_METRIC_NAMES = new HashMap<>();
  static {
    METRIC_NAMES_TO_VARIABLES.put(RESPONSE_TIME_95, "response95th");
    METRIC_NAMES_TO_VARIABLES.put(ERRORS_PER_MINUTE, "error");
    METRIC_NAMES_TO_VARIABLES.put(STALL_COUNT, "stalls");
    METRIC_NAMES_TO_VARIABLES.put(NUMBER_OF_SLOW_CALLS, "slowCalls");

    VARIABLES_TO_METRIC_NAMES.put("callsPerMinute", RESPONSE_TIME_95);
    VARIABLES_TO_METRIC_NAMES.put("error", ERRORS_PER_MINUTE);
    VARIABLES_TO_METRIC_NAMES.put("stalls", STALL_COUNT);
    VARIABLES_TO_METRIC_NAMES.put("slowCalls", NUMBER_OF_SLOW_CALLS);
  }

  public static final Set<String> METRICS_TO_TRACK = Collections.unmodifiableSet(
      new HashSet<>(asList(RESPONSE_TIME_95, ERRORS_PER_MINUTE, STALL_COUNT, NUMBER_OF_SLOW_CALLS)));
}
