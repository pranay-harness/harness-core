package software.wings.service.impl.dynatrace;

import software.wings.metrics.MetricType;
import software.wings.metrics.Threshold;
import software.wings.metrics.ThresholdComparisonType;
import software.wings.metrics.ThresholdType;
import software.wings.metrics.TimeSeriesMetricDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by rsingh on 1/29/18.
 */
public enum DynaTraceTimeSeries {
  CLIENT_SIDE_FAILURE_RATE("com.dynatrace.builtin:servicemethod.clientsidefailurerate", DynaTraceAggregationType.AVG,
      null, "clientSideFailureRate"),
  ERROR_COUNT_HTTP_4XX("com.dynatrace.builtin:servicemethod.errorcounthttp4xx", null, null, "errorCountHttp4xx"),
  ERROR_COUNT_HTTP_5XX("com.dynatrace.builtin:servicemethod.errorcounthttp5xx", null, null, "errorCountHttp5xx"),
  REQUEST_PER_MINUTE(
      "com.dynatrace.builtin:servicemethod.requestspermin", DynaTraceAggregationType.COUNT, null, "requestsPerMin"),
  RESPONSE_TIME(
      "com.dynatrace.builtin:servicemethod.responsetime", DynaTraceAggregationType.PERCENTILE, 95, "responseTime"),
  SERVER_SIDE_FAILURE_RATE("com.dynatrace.builtin:servicemethod.serversidefailurerate", DynaTraceAggregationType.AVG,
      null, "serverSideFailureRate");

  private final String timeseriesId;
  private final DynaTraceAggregationType aggregationType;
  private final Integer percentile;
  private final String savedFieldName;

  DynaTraceTimeSeries(
      String timeseriesId, DynaTraceAggregationType aggregationType, Integer percentile, String savedFieldName) {
    this.timeseriesId = timeseriesId;
    this.aggregationType = aggregationType;
    this.percentile = percentile;
    this.savedFieldName = savedFieldName;
  }

  public String getTimeseriesId() {
    return timeseriesId;
  }

  public DynaTraceAggregationType getAggregationType() {
    return aggregationType;
  }

  public Integer getPercentile() {
    return percentile;
  }

  public String getSavedFieldName() {
    return savedFieldName;
  }

  public static DynaTraceTimeSeries getTimeSeries(String timeseriesId) {
    for (DynaTraceTimeSeries timeSeries : DynaTraceTimeSeries.values()) {
      if (timeSeries.getTimeseriesId().equals(timeseriesId)) {
        return timeSeries;
      }
    }

    return null;
  }

  public static Map<String, TimeSeriesMetricDefinition> getDefinitionsToAnalyze() {
    Map<String, TimeSeriesMetricDefinition> rv = new HashMap<>();

    // clientsidefailurerate
    List<Threshold> thresholds = new ArrayList<>();
    thresholds.add(Threshold.builder()
                       .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                       .comparisonType(ThresholdComparisonType.RATIO)
                       .high(1.5)
                       .medium(1.25)
                       .min(0.5)
                       .build());
    thresholds.add(Threshold.builder()
                       .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                       .comparisonType(ThresholdComparisonType.DELTA)
                       .high(10)
                       .medium(5)
                       .min(0)
                       .build());
    rv.put(CLIENT_SIDE_FAILURE_RATE.getSavedFieldName(),
        TimeSeriesMetricDefinition.builder()
            .metricName(CLIENT_SIDE_FAILURE_RATE.getSavedFieldName())
            .metricType(MetricType.ERROR)
            .thresholds(thresholds)
            .build());

    // errorcounthttp4xx
    rv.put(ERROR_COUNT_HTTP_4XX.getSavedFieldName(),
        TimeSeriesMetricDefinition.builder()
            .metricName(ERROR_COUNT_HTTP_4XX.getSavedFieldName())
            .metricType(MetricType.ERROR)
            .thresholds(thresholds)
            .build());

    // errorcounthttp5xx
    rv.put(ERROR_COUNT_HTTP_5XX.getSavedFieldName(),
        TimeSeriesMetricDefinition.builder()
            .metricName(ERROR_COUNT_HTTP_5XX.getSavedFieldName())
            .metricType(MetricType.ERROR)
            .thresholds(thresholds)
            .build());

    // serversidefailurerate
    rv.put(SERVER_SIDE_FAILURE_RATE.getSavedFieldName(),
        TimeSeriesMetricDefinition.builder()
            .metricName(SERVER_SIDE_FAILURE_RATE.getSavedFieldName())
            .metricType(MetricType.ERROR)
            .thresholds(thresholds)
            .build());

    // requestspermin
    thresholds = new ArrayList<>();
    thresholds.add(Threshold.builder()
                       .thresholdType(ThresholdType.ALERT_WHEN_LOWER)
                       .comparisonType(ThresholdComparisonType.RATIO)
                       .high(0.5)
                       .medium(0.75)
                       .min(0.5)
                       .build());
    thresholds.add(Threshold.builder()
                       .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                       .comparisonType(ThresholdComparisonType.DELTA)
                       .high(100)
                       .medium(50)
                       .min(20)
                       .build());

    rv.put(REQUEST_PER_MINUTE.getSavedFieldName(),
        TimeSeriesMetricDefinition.builder()
            .metricName(REQUEST_PER_MINUTE.getSavedFieldName())
            .metricType(MetricType.THROUGHPUT)
            .thresholds(thresholds)
            .build());

    // responsetime
    thresholds = new ArrayList<>();
    thresholds.add(Threshold.builder()
                       .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                       .comparisonType(ThresholdComparisonType.RATIO)
                       .high(1.5)
                       .medium(1.25)
                       .min(0.5)
                       .build());
    thresholds.add(Threshold.builder()
                       .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                       .comparisonType(ThresholdComparisonType.DELTA)
                       .high(10)
                       .medium(5)
                       .min(50)
                       .build());

    rv.put(RESPONSE_TIME.getSavedFieldName(),
        TimeSeriesMetricDefinition.builder()
            .metricName(RESPONSE_TIME.getSavedFieldName())
            .metricType(MetricType.RESP_TIME)
            .thresholds(thresholds)
            .build());

    return rv;
  }

  public enum DynaTraceAggregationType { MIN, MAX, AVG, SUM, MEDIAN, COUNT, PERCENTILE }
}
