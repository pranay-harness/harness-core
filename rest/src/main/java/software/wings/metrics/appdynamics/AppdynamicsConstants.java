package software.wings.metrics.appdynamics;

import software.wings.metrics.MetricDefinition.ThresholdType;
import software.wings.metrics.MetricType;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by mike@ on 6/14/17.
 */
public class AppdynamicsConstants {
  public static final Set<String> METRICS_TO_TRACK =
      Collections.unmodifiableSet(new HashSet<>(Arrays.asList("Calls per Minute", "95th Percentile Response Time (ms)",
          "Errors per Minute", "Stall Count", "Number of Slow Calls", "Number of Very Slow Calls")));

  // You will need to add the relevant account, appdynamics app, and metric IDs to the metrics to make them useful.
  public static final AppdynamicsMetricDefinition.Builder CALLS_PER_MINUTE_METRIC_TEMPLATE =
      AppdynamicsMetricDefinition.Builder.anAppdynamicsMetricDefinition()
          .withMetricName("Calls per Minute")
          .withMetricType(MetricType.RATE)
          .withThresholdType(ThresholdType.NO_ALERT);

  public static final AppdynamicsMetricDefinition.Builder TOTAL_CALLS_METRIC_TEMPLATE =
      AppdynamicsMetricDefinition.Builder.anAppdynamicsMetricDefinition()
          .withMetricName("Total Calls")
          .withMetricType(MetricType.COUNT)
          .withThresholdType(ThresholdType.NO_ALERT);

  public static final AppdynamicsMetricDefinition.Builder RESPONSE_TIME_95_METRIC_TEMPLATE =
      AppdynamicsMetricDefinition.Builder.anAppdynamicsMetricDefinition()
          .withMetricName("95th Percentile Response Time (ms)")
          .withMetricType(MetricType.TIME)
          .withMediumThreshold(1.0)
          .withHighThreshold(2.0)
          .withThresholdType(ThresholdType.ALERT_WHEN_HIGHER);

  public static final AppdynamicsMetricDefinition.Builder ERRORS_PER_MINUTE_METRIC_TEMPLATE =
      AppdynamicsMetricDefinition.Builder.anAppdynamicsMetricDefinition()
          .withMetricName("Errors per Minute")
          .withMetricType(MetricType.RATE)
          .withMediumThreshold(1.0)
          .withHighThreshold(2.0)
          .withThresholdType(ThresholdType.ALERT_WHEN_HIGHER);

  public static final AppdynamicsMetricDefinition.Builder STALL_COUNT_METRIC_TEMPLATE =
      AppdynamicsMetricDefinition.Builder.anAppdynamicsMetricDefinition()
          .withMetricName("Stall Count")
          .withMetricType(MetricType.COUNT)
          .withMediumThreshold(1.0)
          .withHighThreshold(2.0)
          .withThresholdType(ThresholdType.ALERT_WHEN_HIGHER);

  public static final AppdynamicsMetricDefinition.Builder NUMBER_OF_SLOW_CALLS_METRIC_TEMPLATE =
      AppdynamicsMetricDefinition.Builder.anAppdynamicsMetricDefinition()
          .withMetricName("Number of Slow Calls")
          .withMetricType(MetricType.COUNT)
          .withMediumThreshold(1.0)
          .withHighThreshold(2.0)
          .withThresholdType(ThresholdType.ALERT_WHEN_HIGHER);

  public static final AppdynamicsMetricDefinition.Builder NUMBER_OF_VERY_SLOW_CALLS_METRIC_TEMPLATE =
      AppdynamicsMetricDefinition.Builder.anAppdynamicsMetricDefinition()
          .withMetricName("Number of Very Slow Calls")
          .withMetricType(MetricType.COUNT)
          .withMediumThreshold(1.0)
          .withHighThreshold(2.0)
          .withThresholdType(ThresholdType.ALERT_WHEN_HIGHER);

  public static final Map<String, AppdynamicsMetricDefinition.Builder> METRIC_TEMPLATE_MAP =
      Collections.unmodifiableMap(
          Stream
              .of(new SimpleImmutableEntry<>("Calls per Minute", CALLS_PER_MINUTE_METRIC_TEMPLATE),
                  new SimpleImmutableEntry<>("Total Calls", TOTAL_CALLS_METRIC_TEMPLATE),
                  new SimpleImmutableEntry<>("95th Percentile Response Time (ms)", RESPONSE_TIME_95_METRIC_TEMPLATE),
                  new SimpleImmutableEntry<>("Errors per Minute", ERRORS_PER_MINUTE_METRIC_TEMPLATE),
                  new SimpleImmutableEntry<>("Stall Count", STALL_COUNT_METRIC_TEMPLATE),
                  new SimpleImmutableEntry<>("Number of Slow Calls", NUMBER_OF_SLOW_CALLS_METRIC_TEMPLATE),
                  new SimpleImmutableEntry<>("Number of Very Slow Calls", NUMBER_OF_VERY_SLOW_CALLS_METRIC_TEMPLATE))
              .collect(Collectors.toMap(SimpleImmutableEntry::getKey, SimpleImmutableEntry::getValue)));
}
