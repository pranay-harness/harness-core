package software.wings.service.impl.verification;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.junit.Assert.assertEquals;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;
import software.wings.WingsBaseTest;
import software.wings.dl.WingsPersistence;
import software.wings.metrics.MetricType;
import software.wings.metrics.Threshold;
import software.wings.metrics.ThresholdComparisonType;
import software.wings.metrics.ThresholdType;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.analysis.TimeSeriesMLTransactionThresholds;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.sm.StateType;

import java.util.List;
import java.util.UUID;

/**
 * Created by rsingh on 9/7/18.
 */
public class MetricDataAnalysisServiceTest extends WingsBaseTest {
  private String accountId;
  private String appId;
  private String stateExecutionId;
  private String workflowId;
  private String workflowExecutionId;
  private String serviceId;
  private String cvConfigId;
  private String groupName;
  private String delegateTaskId;
  private Integer analysisMinute;

  @Inject private WingsPersistence wingsPersistence;
  @Inject private MetricDataAnalysisService metricDataAnalysisService;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    accountId = generateUuid();
    appId = generateUuid();
    stateExecutionId = generateUuid();
    workflowId = generateUuid();
    workflowExecutionId = generateUuid();
    serviceId = generateUuid();
    cvConfigId = generateUuid();
    groupName = "groupName-";
    delegateTaskId = UUID.randomUUID().toString();
    analysisMinute = 10;
  }

  @Test
  @Category(UnitTests.class)
  public void testSavingCustomThresholds() {
    int numOfTransactions = 5;
    int numOfMetricsPerTxns = 10;
    for (int i = 0; i < numOfTransactions; i++) {
      for (int j = 0; j < numOfMetricsPerTxns; j++) {
        metricDataAnalysisService.saveCustomThreshold(appId, StateType.NEW_RELIC, serviceId, cvConfigId,
            "transaction-" + (i * numOfMetricsPerTxns + j), DEFAULT_GROUP_NAME,
            TimeSeriesMetricDefinition.builder()
                .metricName("metric-" + (i * numOfMetricsPerTxns + j))
                .metricType(MetricType.THROUGHPUT)
                .customThresholds(Lists.newArrayList(Threshold.builder()
                                                         .comparisonType(ThresholdComparisonType.DELTA)
                                                         .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                                                         .ml(i * numOfMetricsPerTxns + j)
                                                         .build()))
                .build());
      }
    }

    List<TimeSeriesMLTransactionThresholds> thresholds =
        wingsPersistence.createQuery(TimeSeriesMLTransactionThresholds.class)
            .filter("appId", this.appId)
            .order(TimeSeriesMLTransactionThresholds.CREATED_AT_KEY)
            .asList();
    assertEquals(numOfTransactions * numOfMetricsPerTxns, thresholds.size());
    for (int i = 0; i < numOfTransactions * numOfMetricsPerTxns; i++) {
      TimeSeriesMLTransactionThresholds threshold = thresholds.get(i);
      assertEquals(serviceId, threshold.getServiceId());
      assertEquals(StateType.NEW_RELIC, threshold.getStateType());
      assertEquals(DEFAULT_GROUP_NAME, threshold.getGroupName());
      assertEquals("transaction-" + i, threshold.getTransactionName());
      assertEquals("metric-" + i, threshold.getMetricName());
      assertEquals(cvConfigId, threshold.getCvConfigId());
      assertEquals(0, threshold.getVersion());
      assertEquals("metric-" + i, threshold.getThresholds().getMetricName());
      assertEquals(MetricType.THROUGHPUT, threshold.getThresholds().getMetricType());
      assertEquals(1, threshold.getThresholds().getCustomThresholds().size());
      assertEquals(
          ThresholdType.ALERT_WHEN_HIGHER, threshold.getThresholds().getCustomThresholds().get(0).getThresholdType());
      assertEquals(
          ThresholdComparisonType.DELTA, threshold.getThresholds().getCustomThresholds().get(0).getComparisonType());
      assertEquals(i, threshold.getThresholds().getCustomThresholds().get(0).getMl(), 0.0);
    }

    // change a few thresholds
    for (int i = 0; i < 10; i++) {
      metricDataAnalysisService.saveCustomThreshold(appId, StateType.NEW_RELIC, serviceId, cvConfigId,
          "transaction-" + 21, DEFAULT_GROUP_NAME,
          TimeSeriesMetricDefinition.builder()
              .metricName("metric-" + 21)
              .metricType(MetricType.THROUGHPUT)
              .customThresholds(Lists.newArrayList(Threshold.builder()
                                                       .comparisonType(ThresholdComparisonType.DELTA)
                                                       .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                                                       .ml(2100)
                                                       .build()))
              .build());
    }

    for (int i = 0; i < 7; i++) {
      metricDataAnalysisService.saveCustomThreshold(appId, StateType.NEW_RELIC, serviceId, cvConfigId,
          "transaction-" + 37, DEFAULT_GROUP_NAME,
          TimeSeriesMetricDefinition.builder()
              .metricName("metric-" + 37)
              .metricType(MetricType.THROUGHPUT)
              .customThresholds(Lists.newArrayList(Threshold.builder()
                                                       .comparisonType(ThresholdComparisonType.DELTA)
                                                       .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                                                       .ml(3700)
                                                       .build()))
              .build());
    }

    thresholds = wingsPersistence.createQuery(TimeSeriesMLTransactionThresholds.class)
                     .filter("appId", this.appId)
                     .order(TimeSeriesMLTransactionThresholds.CREATED_AT_KEY)
                     .asList();
    assertEquals(numOfTransactions * numOfMetricsPerTxns, thresholds.size());
    for (int i = 0; i < numOfTransactions * numOfMetricsPerTxns; i++) {
      TimeSeriesMLTransactionThresholds threshold = thresholds.get(i);
      assertEquals(serviceId, threshold.getServiceId());
      assertEquals(StateType.NEW_RELIC, threshold.getStateType());
      assertEquals(DEFAULT_GROUP_NAME, threshold.getGroupName());
      assertEquals("transaction-" + i, threshold.getTransactionName());
      assertEquals("metric-" + i, threshold.getMetricName());
      assertEquals(cvConfigId, threshold.getCvConfigId());
      assertEquals("metric-" + i, threshold.getThresholds().getMetricName());
      assertEquals(MetricType.THROUGHPUT, threshold.getThresholds().getMetricType());
      assertEquals(1, threshold.getThresholds().getCustomThresholds().size());
      assertEquals(
          ThresholdType.ALERT_WHEN_HIGHER, threshold.getThresholds().getCustomThresholds().get(0).getThresholdType());
      assertEquals(
          ThresholdComparisonType.DELTA, threshold.getThresholds().getCustomThresholds().get(0).getComparisonType());

      if (i == 21) {
        assertEquals(10, threshold.getVersion());
        assertEquals(2100, threshold.getThresholds().getCustomThresholds().get(0).getMl(), 0.0);
      } else if (i == 37) {
        assertEquals(7, threshold.getVersion());
        assertEquals(3700, threshold.getThresholds().getCustomThresholds().get(0).getMl(), 0.0);
      } else {
        assertEquals(0, threshold.getVersion());
        assertEquals(i, threshold.getThresholds().getCustomThresholds().get(0).getMl(), 0.0);
      }
    }
  }
}
