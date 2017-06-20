package software.wings.metrics.appdynamics;

import software.wings.metrics.MetricDefinition.Threshold;
import software.wings.metrics.MetricDefinition.ThresholdType;
import software.wings.metrics.MetricType;
import software.wings.metrics.ThresholdComparisonType;

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
          .withThreshold(ThresholdComparisonType.RATIO, new Threshold(ThresholdType.NO_ALERT, 1, 2));

  public static final AppdynamicsMetricDefinition.Builder TOTAL_CALLS_METRIC_TEMPLATE =
      AppdynamicsMetricDefinition.Builder.anAppdynamicsMetricDefinition()
          .withMetricName(TOTAL_CALLS)
          .withMetricType(MetricType.COUNT)
          .withThreshold(ThresholdComparisonType.RATIO, new Threshold(ThresholdType.NO_ALERT, 1, 2));

  public static final AppdynamicsMetricDefinition.Builder RESPONSE_TIME_95_METRIC_TEMPLATE =
      AppdynamicsMetricDefinition.Builder.anAppdynamicsMetricDefinition()
          .withMetricName(RESPONSE_TIME_95)
          .withMetricType(MetricType.TIME_MS)
          .withThreshold(ThresholdComparisonType.RATIO, new Threshold(ThresholdType.ALERT_WHEN_HIGHER, 1.0, 1.3))
          .withThreshold(ThresholdComparisonType.DELTA, new Threshold(ThresholdType.ALERT_WHEN_HIGHER, 0.01, 20.0));

  public static final AppdynamicsMetricDefinition.Builder ERRORS_PER_MINUTE_METRIC_TEMPLATE =
      AppdynamicsMetricDefinition.Builder.anAppdynamicsMetricDefinition()
          .withMetricName(ERRORS_PER_MINUTE)
          .withMetricType(MetricType.RATE)
          .withThreshold(ThresholdComparisonType.RATIO, new Threshold(ThresholdType.ALERT_WHEN_HIGHER, 1.0, 1.3))
          .withThreshold(ThresholdComparisonType.ABSOLUTE, new Threshold(ThresholdType.ALERT_WHEN_HIGHER, 3.0, 5.0));

  public static final AppdynamicsMetricDefinition.Builder STALL_COUNT_METRIC_TEMPLATE =
      AppdynamicsMetricDefinition.Builder.anAppdynamicsMetricDefinition()
          .withMetricName(STALL_COUNT)
          .withMetricType(MetricType.COUNT)
          .withThreshold(ThresholdComparisonType.RATIO, new Threshold(ThresholdType.ALERT_WHEN_HIGHER, 1.0, 1.3));

  public static final AppdynamicsMetricDefinition.Builder NUMBER_OF_SLOW_CALLS_METRIC_TEMPLATE =
      AppdynamicsMetricDefinition.Builder.anAppdynamicsMetricDefinition()
          .withMetricName(NUMBER_OF_SLOW_CALLS)
          .withMetricType(MetricType.COUNT)
          .withThreshold(ThresholdComparisonType.RATIO, new Threshold(ThresholdType.ALERT_WHEN_HIGHER, 1.0, 1.3));

  public static final AppdynamicsMetricDefinition.Builder NUMBER_OF_VERY_SLOW_CALLS_METRIC_TEMPLATE =
      AppdynamicsMetricDefinition.Builder.anAppdynamicsMetricDefinition()
          .withMetricName(NUMBER_OF_VERY_SLOW_CALLS)
          .withMetricType(MetricType.COUNT)
          .withThreshold(ThresholdComparisonType.RATIO, new Threshold(ThresholdType.ALERT_WHEN_HIGHER, 1.0, 1.3));

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
