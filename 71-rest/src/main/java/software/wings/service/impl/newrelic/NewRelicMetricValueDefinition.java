package software.wings.service.impl.newrelic;

import static io.harness.beans.SortOrder.Builder.aSortOrder;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.common.collect.ImmutableMap;
import com.google.common.math.Stats;

import io.harness.beans.SortOrder;
import io.harness.beans.SortOrder.OrderType;
import lombok.Builder;
import lombok.Data;
import software.wings.metrics.MetricType;
import software.wings.metrics.RiskLevel;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.appdynamics.AppdynamicsTimeSeries;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricAnalysisValue;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * Created by rsingh on 9/6/17.
 */
@Data
@Builder
public class NewRelicMetricValueDefinition {
  public static String ERROR = "error";
  public static String THROUGHPUT = "throughput";
  public static String AVERAGE_RESPONSE_TIME = "averageResponseTime";
  public static String APDEX_SCORE = "apdexScore";
  public static String CALL_COUNT = "callCount";
  public static String REQUSET_PER_MINUTE = "requestsPerMinute";

  public static String RESPONSE_TIME_95 = "response95th";
  public static String STALL_COUNT = "stalls";
  public static String NUMBER_OF_SLOW_CALLS = "slowCalls";

  public static String CLIENT_SIDE_FAILURE_RATE = "clientSideFailureRate";
  public static String ERROR_COUNT_HTTP_4XX = "errorCountHttp4xx";
  public static String ERROR_COUNT_HTTP_5XX = "errorCountHttp5xx";
  public static String REQUEST_PER_MINUTE = "requestsPerMin";
  public static String RESPONSE_TIME = "responseTime";
  public static String SERVER_SIDE_FAILURE_RATE = "serverSideFailureRate";

  public static Map<StateType, SortOrder> SORTING_METRIC_NAME =
      ImmutableMap.of(StateType.APP_DYNAMICS, aSortOrder().withField(RESPONSE_TIME_95, OrderType.DESC).build(),
          StateType.NEW_RELIC, aSortOrder().withField(REQUSET_PER_MINUTE, OrderType.DESC).build(), StateType.DYNA_TRACE,
          aSortOrder().withField(REQUEST_PER_MINUTE, OrderType.DESC).build());

  public static Map<String, TimeSeriesMetricDefinition> NEW_RELIC_VALUES_TO_ANALYZE = new HashMap<>();

  static {
    NEW_RELIC_VALUES_TO_ANALYZE.put(REQUSET_PER_MINUTE,
        TimeSeriesMetricDefinition.builder().metricName(REQUSET_PER_MINUTE).metricType(MetricType.THROUGHPUT).build());

    NEW_RELIC_VALUES_TO_ANALYZE.put(AVERAGE_RESPONSE_TIME,
        TimeSeriesMetricDefinition.builder()
            .metricName(AVERAGE_RESPONSE_TIME)
            .metricType(MetricType.RESP_TIME)
            .build());

    NEW_RELIC_VALUES_TO_ANALYZE.put(
        ERROR, TimeSeriesMetricDefinition.builder().metricName(ERROR).metricType(MetricType.ERROR).build());

    NEW_RELIC_VALUES_TO_ANALYZE.put(
        APDEX_SCORE, TimeSeriesMetricDefinition.builder().metricName(APDEX_SCORE).metricType(MetricType.VALUE).build());
  }

  public static Map<String, TimeSeriesMetricDefinition> APP_DYNAMICS_VALUES_TO_ANALYZE = new HashMap<>();
  static {
    // 95th percentile response time
    String metricName = AppdynamicsTimeSeries.RESPONSE_TIME_95.getMetricName();
    APP_DYNAMICS_VALUES_TO_ANALYZE.put(metricName,
        TimeSeriesMetricDefinition.builder().metricName(metricName).metricType(MetricType.RESP_TIME).build());

    // slow calls
    metricName = AppdynamicsTimeSeries.NUMBER_OF_SLOW_CALLS.getMetricName();
    APP_DYNAMICS_VALUES_TO_ANALYZE.put(
        metricName, TimeSeriesMetricDefinition.builder().metricName(metricName).metricType(MetricType.ERROR).build());

    // error
    metricName = AppdynamicsTimeSeries.ERRORS_PER_MINUTE.getMetricName();
    APP_DYNAMICS_VALUES_TO_ANALYZE.put(
        metricName, TimeSeriesMetricDefinition.builder().metricName(metricName).metricType(MetricType.ERROR).build());

    // stalls
    metricName = AppdynamicsTimeSeries.STALL_COUNT.getMetricName();
    APP_DYNAMICS_VALUES_TO_ANALYZE.put(
        metricName, TimeSeriesMetricDefinition.builder().metricName(metricName).metricType(MetricType.ERROR).build());
  }

  private String metricName;
  private String metricValueName;
  private MetricType metricType;

  public NewRelicMetricAnalysisValue analyze(
      List<NewRelicMetricDataRecord> testRecords, List<NewRelicMetricDataRecord> controlRecords) {
    double testValue = getValueForComparison(testRecords);
    double controlValue = getValueForComparison(controlRecords);

    return NewRelicMetricAnalysisValue.builder()
        .name(metricValueName)
        .riskLevel(RiskLevel.NA)
        .controlValue(controlValue)
        .testValue(testValue)
        .build();
  }

  private double getValueForComparison(List<NewRelicMetricDataRecord> records) {
    double value;
    if (isEmpty(records)) {
      value = -1;
    } else {
      List<Double> testValues;
      try {
        testValues = parseValuesForAnalysis(records);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      if (testValues.isEmpty()) {
        value = -1;
      } else {
        value = Stats.of(testValues).sum() / testValues.size();
      }
    }
    return value;
  }

  private List<Double> parseValuesForAnalysis(List<NewRelicMetricDataRecord> records) {
    List<Double> values = new ArrayList<>();
    for (NewRelicMetricDataRecord metricDataRecord : records) {
      if (metricDataRecord.getValues() == null) {
        continue;
      }

      Double value = metricDataRecord.getValues().get(metricValueName);
      if (value != null) {
        values.add(value);
      }
    }

    return values;
  }
}
