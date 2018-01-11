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

  public static Map<String, String> METRIC_NAMES_TO_VARIABLES = new HashMap<String, String>() {
    {
      put(RESPONSE_TIME_95, "response95th");
      put(ERRORS_PER_MINUTE, "error");
      put(STALL_COUNT, "stalls");
      put(NUMBER_OF_SLOW_CALLS, "slowCalls");
    }
  };

  public static Map<String, String> VARIABLES_TO_METRIC_NAMES = new HashMap<String, String>() {
    {
      put("callsPerMinute", RESPONSE_TIME_95);
      put("error", ERRORS_PER_MINUTE);
      put("stalls", STALL_COUNT);
      put("slowCalls", NUMBER_OF_SLOW_CALLS);
    }
  };

  public static final Set<String> METRICS_TO_TRACK = Collections.unmodifiableSet(
      new HashSet<>(asList(RESPONSE_TIME_95, ERRORS_PER_MINUTE, STALL_COUNT, NUMBER_OF_SLOW_CALLS)));
}
