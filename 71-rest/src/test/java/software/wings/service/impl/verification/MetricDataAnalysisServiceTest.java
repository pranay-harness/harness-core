package software.wings.service.impl.verification;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRAVEEN;
import static io.harness.rule.OwnerRule.RAGHU;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
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
import software.wings.service.impl.analysis.TimeSeriesMLTransactionThresholds.TimeSeriesMLTransactionThresholdKeys;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.sm.StateType;

import java.util.Arrays;
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
  @Owner(developers = RAGHU)
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
    assertThat(thresholds).hasSize(numOfTransactions * numOfMetricsPerTxns);
    for (int i = 0; i < numOfTransactions * numOfMetricsPerTxns; i++) {
      TimeSeriesMLTransactionThresholds threshold = thresholds.get(i);
      assertThat(threshold.getServiceId()).isEqualTo(serviceId);
      assertThat(threshold.getStateType()).isEqualTo(StateType.NEW_RELIC);
      assertThat(threshold.getGroupName()).isEqualTo(DEFAULT_GROUP_NAME);
      assertThat(threshold.getTransactionName()).isEqualTo("transaction-" + i);
      assertThat(threshold.getMetricName()).isEqualTo("metric-" + i);
      assertThat(threshold.getCvConfigId()).isEqualTo(cvConfigId);
      assertThat(threshold.getVersion()).isEqualTo(0);
      assertThat(threshold.getThresholds().getMetricName()).isEqualTo("metric-" + i);
      assertThat(threshold.getThresholds().getMetricType()).isEqualTo(MetricType.THROUGHPUT);
      assertThat(threshold.getThresholds().getCustomThresholds()).hasSize(1);
      assertThat(threshold.getThresholds().getCustomThresholds().get(0).getThresholdType())
          .isEqualTo(ThresholdType.ALERT_WHEN_HIGHER);
      assertThat(threshold.getThresholds().getCustomThresholds().get(0).getComparisonType())
          .isEqualTo(ThresholdComparisonType.DELTA);
      assertThat(threshold.getThresholds().getCustomThresholds().get(0).getMl()).isEqualTo(i);
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
    assertThat(thresholds).hasSize(numOfTransactions * numOfMetricsPerTxns);
    for (int i = 0; i < numOfTransactions * numOfMetricsPerTxns; i++) {
      TimeSeriesMLTransactionThresholds threshold = thresholds.get(i);
      assertThat(threshold.getServiceId()).isEqualTo(serviceId);
      assertThat(threshold.getStateType()).isEqualTo(StateType.NEW_RELIC);
      assertThat(threshold.getGroupName()).isEqualTo(DEFAULT_GROUP_NAME);
      assertThat(threshold.getTransactionName()).isEqualTo("transaction-" + i);
      assertThat(threshold.getMetricName()).isEqualTo("metric-" + i);
      assertThat(threshold.getCvConfigId()).isEqualTo(cvConfigId);
      assertThat(threshold.getThresholds().getMetricName()).isEqualTo("metric-" + i);
      assertThat(threshold.getThresholds().getMetricType()).isEqualTo(MetricType.THROUGHPUT);
      assertThat(threshold.getThresholds().getCustomThresholds()).hasSize(1);
      assertThat(threshold.getThresholds().getCustomThresholds().get(0).getThresholdType())
          .isEqualTo(ThresholdType.ALERT_WHEN_HIGHER);
      assertThat(threshold.getThresholds().getCustomThresholds().get(0).getComparisonType())
          .isEqualTo(ThresholdComparisonType.DELTA);

      if (i == 21) {
        assertThat(threshold.getVersion()).isEqualTo(10);
        assertThat(threshold.getThresholds().getCustomThresholds().get(0).getMl()).isEqualTo(2100);
      } else if (i == 37) {
        assertThat(threshold.getVersion()).isEqualTo(7);
        assertThat(threshold.getThresholds().getCustomThresholds().get(0).getMl()).isEqualTo(3700);
      } else {
        assertThat(threshold.getVersion()).isEqualTo(0);
        assertThat(threshold.getThresholds().getCustomThresholds().get(0).getMl()).isEqualTo(i);
      }
    }
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testDeleteCustomThresholdsWithoutComparisonType() throws Exception {
    TimeSeriesMLTransactionThresholds timeSeriesMLTransactionThresholds =
        TimeSeriesMLTransactionThresholds.builder()
            .stateType(StateType.NEW_RELIC)
            .groupName(null)
            .serviceId(serviceId)
            .cvConfigId(cvConfigId)
            .transactionName("transaction-name")
            .metricName("metric-name")
            .thresholds(TimeSeriesMetricDefinition.builder()
                            .metricName("metric-name")
                            .metricType(MetricType.THROUGHPUT)
                            .customThresholds(Arrays.asList(Threshold.builder()
                                                                .comparisonType(ThresholdComparisonType.DELTA)
                                                                .thresholdType(ThresholdType.ALERT_WHEN_LOWER)
                                                                .ml(10)
                                                                .build()))
                            .build())
            .build();
    timeSeriesMLTransactionThresholds.setAppId(appId);
    wingsPersistence.save(timeSeriesMLTransactionThresholds);
    List<TimeSeriesMLTransactionThresholds> thresholds =
        wingsPersistence.createQuery(TimeSeriesMLTransactionThresholds.class)
            .filter(TimeSeriesMLTransactionThresholdKeys.cvConfigId, cvConfigId)
            .asList();

    assertThat(thresholds.size()).isEqualTo(1);
    metricDataAnalysisService.deleteCustomThreshold(
        appId, StateType.NEW_RELIC, serviceId, cvConfigId, null, "transaction-name", "metric-name", null);
    thresholds = wingsPersistence.createQuery(TimeSeriesMLTransactionThresholds.class)
                     .filter(TimeSeriesMLTransactionThresholdKeys.cvConfigId, cvConfigId)
                     .asList();
    assertThat(thresholds).isEmpty();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testDeleteCustomThresholds() throws Exception {
    TimeSeriesMLTransactionThresholds timeSeriesMLTransactionThresholds =
        TimeSeriesMLTransactionThresholds.builder()
            .stateType(StateType.NEW_RELIC)
            .groupName(null)
            .serviceId(serviceId)
            .cvConfigId(cvConfigId)
            .transactionName("transaction-name")
            .metricName("metric-name")
            .thresholds(TimeSeriesMetricDefinition.builder()
                            .metricName("metric-name")
                            .metricType(MetricType.THROUGHPUT)
                            .customThresholds(Arrays.asList(Threshold.builder()
                                                                .comparisonType(ThresholdComparisonType.DELTA)
                                                                .thresholdType(ThresholdType.ALERT_WHEN_LOWER)
                                                                .ml(10)
                                                                .build()))
                            .build())
            .build();
    timeSeriesMLTransactionThresholds.setAppId(appId);

    TimeSeriesMLTransactionThresholds timeSeriesMLTransactionThresholds2 =
        TimeSeriesMLTransactionThresholds.builder()
            .stateType(StateType.NEW_RELIC)
            .groupName(null)
            .serviceId(serviceId)
            .cvConfigId(cvConfigId)
            .transactionName("transaction-name")
            .metricName("metric-name")
            .thresholds(TimeSeriesMetricDefinition.builder()
                            .metricName("metric-name")
                            .metricType(MetricType.THROUGHPUT)
                            .customThresholds(Arrays.asList(Threshold.builder()
                                                                .comparisonType(ThresholdComparisonType.ABSOLUTE)
                                                                .thresholdType(ThresholdType.ALERT_WHEN_LOWER)
                                                                .ml(10)
                                                                .build()))
                            .build())
            .build();
    timeSeriesMLTransactionThresholds2.setAppId(appId);
    metricDataAnalysisService.saveCustomThreshold(
        serviceId, cvConfigId, Arrays.asList(timeSeriesMLTransactionThresholds, timeSeriesMLTransactionThresholds2));
    List<TimeSeriesMLTransactionThresholds> thresholds =
        wingsPersistence.createQuery(TimeSeriesMLTransactionThresholds.class)
            .filter(TimeSeriesMLTransactionThresholdKeys.cvConfigId, cvConfigId)
            .asList();

    assertThat(thresholds.size()).isEqualTo(2);
    assertThat(thresholds.get(0).getTransactionName()).isEqualTo("transaction-name");
    assertThat(thresholds.get(0).getMetricName()).isEqualTo("metric-name");
    assertThat(thresholds.get(1).getTransactionName()).isEqualTo("transaction-name");
    assertThat(thresholds.get(1).getMetricName()).isEqualTo("metric-name");
    assertThat(thresholds.get(0).getThresholds().getCustomThresholds().get(0).getComparisonType())
        .isIn(Arrays.asList(ThresholdComparisonType.ABSOLUTE, ThresholdComparisonType.DELTA));
    assertThat(thresholds.get(1).getThresholds().getCustomThresholds().get(0).getComparisonType())
        .isIn(Arrays.asList(ThresholdComparisonType.ABSOLUTE, ThresholdComparisonType.DELTA));
    assertThat(thresholds.get(0).getThresholds().getCustomThresholds().get(0).getComparisonType())
        .isNotEqualByComparingTo(thresholds.get(1).getThresholds().getCustomThresholds().get(0).getComparisonType());
    metricDataAnalysisService.deleteCustomThreshold(appId, StateType.NEW_RELIC, serviceId, cvConfigId, null,
        "transaction-name", "metric-name", ThresholdComparisonType.ABSOLUTE);

    thresholds = wingsPersistence.createQuery(TimeSeriesMLTransactionThresholds.class)
                     .filter(TimeSeriesMLTransactionThresholdKeys.cvConfigId, cvConfigId)
                     .asList();

    assertThat(thresholds.size()).isEqualTo(1);
    assertThat(thresholds.get(0).getThresholds().getCustomThresholds().iterator().next().getComparisonType())
        .isEqualTo(ThresholdComparisonType.DELTA);
  }
}
