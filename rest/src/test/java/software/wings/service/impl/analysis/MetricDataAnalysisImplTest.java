package software.wings.service.impl.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import software.wings.WingsBaseTest;
import software.wings.dl.WingsPersistence;
import software.wings.metrics.MetricType;
import software.wings.metrics.Threshold;
import software.wings.metrics.ThresholdComparisonType;
import software.wings.metrics.ThresholdType;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.sm.StateType;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class MetricDataAnalysisImplTest extends WingsBaseTest {
  private String accountId;
  private String appId;
  private String stateExecutionId;
  private String workflowId;
  private String workflowExecutionId;
  private String serviceId;
  private String groupName;
  private String delegateTaskId;
  private String analysisServerConfigId;
  private String correlationId;
  private String preWorkflowExecutionId;

  @Inject private WingsPersistence wingsPersistence;
  @Inject private MetricDataAnalysisService metricDataAnalysisService;

  @Before
  public void setup() throws IOException {
    MockitoAnnotations.initMocks(this);
    accountId = UUID.randomUUID().toString();
    appId = UUID.randomUUID().toString();
    stateExecutionId = UUID.randomUUID().toString();
    workflowId = UUID.randomUUID().toString();
    workflowExecutionId = UUID.randomUUID().toString();
    serviceId = UUID.randomUUID().toString();
    groupName = "groupName-";
    delegateTaskId = UUID.randomUUID().toString();
    analysisServerConfigId = UUID.randomUUID().toString();
    correlationId = UUID.randomUUID().toString();
    preWorkflowExecutionId = UUID.randomUUID().toString();
  }

  @Test
  public void testSaveCustomThreshold() {
    Threshold threshold = Threshold.builder()
                              .comparisonType(ThresholdComparisonType.ABSOLUTE)
                              .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                              .ml(500)
                              .build();

    metricDataAnalysisService.saveCustomThreshold(appId, StateType.NEW_RELIC, serviceId, "default", "txn1",
        TimeSeriesMetricDefinition.builder()
            .metricName("metric1")
            .metricType(MetricType.ERROR)
            .customThresholds(Lists.newArrayList(threshold))
            .build());
    assertNotNull(metricDataAnalysisService.getCustomThreshold(
        appId, StateType.NEW_RELIC, serviceId, "default", "txn1", "metric1"));

    Map<String, Map<String, TimeSeriesMetricDefinition>> metricTemplates =
        metricDataAnalysisService.getMetricTemplate(appId, StateType.NEW_RELIC, stateExecutionId, serviceId, "default");

    assertNotNull(metricTemplates.get("txn1"));
    assertEquals(metricTemplates.get("txn1").get("metric1").getCustomThresholds().get(0), threshold);

    metricDataAnalysisService.deleteCustomThreshold(
        appId, StateType.NEW_RELIC, serviceId, "default", "txn1", "metric1");

    assertNull(metricDataAnalysisService.getCustomThreshold(
        appId, StateType.NEW_RELIC, serviceId, "default", "txn1", "metric1"));
  }
}
