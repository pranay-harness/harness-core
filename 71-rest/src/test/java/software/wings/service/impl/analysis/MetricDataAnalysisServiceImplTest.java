package software.wings.service.impl.analysis;

import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRAVEEN;
import static io.harness.rule.OwnerRule.SOWMYA;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.WingsPersistence;
import software.wings.metrics.MetricType;
import software.wings.metrics.Threshold;
import software.wings.metrics.ThresholdComparisonType;
import software.wings.metrics.ThresholdType;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.analysis.TimeSeriesMLTransactionThresholds.TimeSeriesMLTransactionThresholdKeys;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.sm.StateType;

import java.util.Arrays;
import java.util.List;

/**
 * @author Praveen
 *
 */
public class MetricDataAnalysisServiceImplTest extends WingsBaseTest {
  @Inject WingsPersistence wingsPersistence;
  @Inject MetricDataAnalysisService metricDataAnalysisService;

  String appId, workflowId, serviceId, infraMappingId, envId, cvConfigId;
  private String stateExecutionId;

  @Before
  public void setup() throws IllegalAccessException {
    appId = generateUuid();
    serviceId = generateUuid();
    workflowId = generateUuid();
    infraMappingId = generateUuid();
    envId = generateUuid();
    cvConfigId = generateUuid();
    stateExecutionId = generateUuid();
    FieldUtils.writeField(metricDataAnalysisService, "wingsPersistence", wingsPersistence, true);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetLastSuccessfulWorkflowWithData() {
    String execId = generateUuid();
    WorkflowExecution execution = WorkflowExecution.builder()
                                      .appId(appId)
                                      .workflowId(workflowId)
                                      .infraMappingIds(Arrays.asList(infraMappingId))
                                      .serviceIds(Arrays.asList(serviceId))
                                      .envId(envId)
                                      .status(SUCCESS)
                                      .uuid(execId)
                                      .build();

    wingsPersistence.save(execution);

    NewRelicMetricDataRecord record = NewRelicMetricDataRecord.builder()
                                          .stateType(StateType.NEW_RELIC)
                                          .appId(appId)
                                          .workflowId(workflowId)
                                          .workflowExecutionId(execId)
                                          .serviceId(serviceId)
                                          .build();
    wingsPersistence.save(record);

    String lastId = metricDataAnalysisService.getLastSuccessfulWorkflowExecutionIdWithData(
        StateType.NEW_RELIC, appId, workflowId, serviceId, infraMappingId, envId);

    assertThat(lastId).isNotNull();
    assertThat(lastId).isEqualTo(execId);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetLastSuccessfulWorkflowWithDataDifferentInfra() {
    String execId = generateUuid();
    String newInfra = generateUuid();
    WorkflowExecution execution = WorkflowExecution.builder()
                                      .appId(appId)
                                      .workflowId(workflowId)
                                      .infraMappingIds(Arrays.asList(newInfra))
                                      .serviceIds(Arrays.asList(serviceId))
                                      .status(SUCCESS)
                                      .envId(envId)
                                      .uuid(execId)
                                      .build();

    wingsPersistence.save(execution);

    NewRelicMetricDataRecord record = NewRelicMetricDataRecord.builder()
                                          .stateType(StateType.NEW_RELIC)
                                          .appId(appId)
                                          .workflowId(workflowId)
                                          .workflowExecutionId(execId)
                                          .serviceId(serviceId)
                                          .build();
    wingsPersistence.save(record);

    String lastId = metricDataAnalysisService.getLastSuccessfulWorkflowExecutionIdWithData(
        StateType.NEW_RELIC, appId, workflowId, serviceId, infraMappingId, envId);

    assertThat(lastId).isNull();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetCustomThresholds() {
    saveCustomThresholds();

    List<TimeSeriesMLTransactionThresholds> thresholds =
        metricDataAnalysisService.getCustomThreshold(TimeSeriesMLTransactionThresholdKeys.cvConfigId, cvConfigId);
    assertThat(thresholds).isNotEmpty();
    assertThat(thresholds.size()).isEqualTo(50);
  }

  private void saveCustomThresholds() {
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
                .build(),
            null);
      }
    }
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testGetLatestLocalAnalysisRecord_EmptyRecords() {
    NewRelicMetricAnalysisRecord analysisRecord =
        metricDataAnalysisService.getLatestLocalAnalysisRecord(stateExecutionId);
    assertThat(analysisRecord).isNull();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testGetLatestLocalAnalysisRecord_IncorrectId() {
    NewRelicMetricAnalysisRecord record = NewRelicMetricAnalysisRecord.builder()
                                              .stateExecutionId(stateExecutionId + "2")
                                              .analysisMinute(1)
                                              .message("Test message")
                                              .build();
    wingsPersistence.save(record);
    NewRelicMetricAnalysisRecord analysisRecord =
        metricDataAnalysisService.getLatestLocalAnalysisRecord(stateExecutionId);
    assertThat(analysisRecord).isNull();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testGetLatestLocalAnalysisRecord_ExistingRecord() {
    NewRelicMetricAnalysisRecord record = NewRelicMetricAnalysisRecord.builder()
                                              .stateExecutionId(stateExecutionId)
                                              .analysisMinute(1)
                                              .message("The state was marked")
                                              .build();
    record.setUuid(generateUuid());
    wingsPersistence.save(record);
    NewRelicMetricAnalysisRecord analysisRecord =
        metricDataAnalysisService.getLatestLocalAnalysisRecord(stateExecutionId);
    assertThat(analysisRecord).isNotNull();
    assertThat(analysisRecord.getUuid()).isEqualTo(record.getUuid());
  }
}
