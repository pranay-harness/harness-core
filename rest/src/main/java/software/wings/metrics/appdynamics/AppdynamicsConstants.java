package software.wings.metrics.appdynamics;

import software.wings.metrics.MetricDefinition.ThresholdType;
import software.wings.metrics.MetricType;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by mike@ on 6/14/17.
 */
public class AppdynamicsConstants {
  public static final String CALLS_PER_MINUTE = "Calls per Minute";
  public static final String RESPONSE_TIME_95 = "95th Percentile Response Time (ms)";
  public static final String ERRORS_PER_MINUTE = "Errors per Minute";
  public static final String STALL_COUNT = "Stall Count";
  public static final String NUMBER_OF_SLOW_CALLS = "Number of Slow Calls";
  public static final String NUMBER_OF_VERY_SLOW_CALLS = "Number of Very Slow Calls";
  public static final String TOTAL_CALLS = "Total Calls";

  public static final Set<String> METRICS_TO_TRACK =
      Collections.unmodifiableSet(new HashSet<>(Arrays.asList(CALLS_PER_MINUTE, RESPONSE_TIME_95, ERRORS_PER_MINUTE,
          STALL_COUNT, NUMBER_OF_SLOW_CALLS, NUMBER_OF_VERY_SLOW_CALLS, TOTAL_CALLS)));

  // You will need to add the relevant account, appdynamics app, and metric IDs to the metrics to make them useful.
  public static final AppdynamicsMetricDefinition.Builder CALLS_PER_MINUTE_METRIC_TEMPLATE =
      AppdynamicsMetricDefinition.Builder.anAppdynamicsMetricDefinition()
          .withMetricName(CALLS_PER_MINUTE)
          .withMetricType(MetricType.RATE)
          .withThresholdType(ThresholdType.NO_ALERT);

  public static final AppdynamicsMetricDefinition.Builder TOTAL_CALLS_METRIC_TEMPLATE =
      AppdynamicsMetricDefinition.Builder.anAppdynamicsMetricDefinition()
          .withMetricName(TOTAL_CALLS)
          .withMetricType(MetricType.COUNT)
          .withThresholdType(ThresholdType.NO_ALERT);

  public static final AppdynamicsMetricDefinition.Builder RESPONSE_TIME_95_METRIC_TEMPLATE =
      AppdynamicsMetricDefinition.Builder.anAppdynamicsMetricDefinition()
          .withMetricName(RESPONSE_TIME_95)
          .withMetricType(MetricType.TIME_MS)
          .withMediumThreshold(1.0)
          .withHighThreshold(2.0)
          .withThresholdType(ThresholdType.ALERT_WHEN_HIGHER);

  public static final AppdynamicsMetricDefinition.Builder ERRORS_PER_MINUTE_METRIC_TEMPLATE =
      AppdynamicsMetricDefinition.Builder.anAppdynamicsMetricDefinition()
          .withMetricName(ERRORS_PER_MINUTE)
          .withMetricType(MetricType.RATE)
          .withMediumThreshold(1.0)
          .withHighThreshold(2.0)
          .withThresholdType(ThresholdType.ALERT_WHEN_HIGHER);

  public static final AppdynamicsMetricDefinition.Builder STALL_COUNT_METRIC_TEMPLATE =
      AppdynamicsMetricDefinition.Builder.anAppdynamicsMetricDefinition()
          .withMetricName(STALL_COUNT)
          .withMetricType(MetricType.COUNT)
          .withMediumThreshold(1.0)
          .withHighThreshold(2.0)
          .withThresholdType(ThresholdType.ALERT_WHEN_HIGHER);

  public static final AppdynamicsMetricDefinition.Builder NUMBER_OF_SLOW_CALLS_METRIC_TEMPLATE =
      AppdynamicsMetricDefinition.Builder.anAppdynamicsMetricDefinition()
          .withMetricName(NUMBER_OF_SLOW_CALLS)
          .withMetricType(MetricType.COUNT)
          .withMediumThreshold(1.0)
          .withHighThreshold(2.0)
          .withThresholdType(ThresholdType.ALERT_WHEN_HIGHER);

  public static final AppdynamicsMetricDefinition.Builder NUMBER_OF_VERY_SLOW_CALLS_METRIC_TEMPLATE =
      AppdynamicsMetricDefinition.Builder.anAppdynamicsMetricDefinition()
          .withMetricName(NUMBER_OF_VERY_SLOW_CALLS)
          .withMetricType(MetricType.COUNT)
          .withMediumThreshold(1.0)
          .withHighThreshold(2.0)
          .withThresholdType(ThresholdType.ALERT_WHEN_HIGHER);

  public static final Map<String, AppdynamicsMetricDefinition.Builder> METRIC_TEMPLATE_MAP =
      Collections.unmodifiableMap(
          Stream
              .of(new SimpleImmutableEntry<>(CALLS_PER_MINUTE, CALLS_PER_MINUTE_METRIC_TEMPLATE),
                  new SimpleImmutableEntry<>(TOTAL_CALLS, TOTAL_CALLS_METRIC_TEMPLATE),
                  new SimpleImmutableEntry<>(RESPONSE_TIME_95, RESPONSE_TIME_95_METRIC_TEMPLATE),
                  new SimpleImmutableEntry<>(ERRORS_PER_MINUTE, ERRORS_PER_MINUTE_METRIC_TEMPLATE),
                  new SimpleImmutableEntry<>(STALL_COUNT, STALL_COUNT_METRIC_TEMPLATE),
                  new SimpleImmutableEntry<>(NUMBER_OF_SLOW_CALLS, NUMBER_OF_SLOW_CALLS_METRIC_TEMPLATE),
                  new SimpleImmutableEntry<>(NUMBER_OF_VERY_SLOW_CALLS, NUMBER_OF_VERY_SLOW_CALLS_METRIC_TEMPLATE))
              .collect(Collectors.toMap(SimpleImmutableEntry::getKey, SimpleImmutableEntry::getValue)));
}
