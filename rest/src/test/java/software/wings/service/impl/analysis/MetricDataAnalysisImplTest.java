package software.wings.service.impl.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MetricDataAnalysisImplTest extends WingsBaseTest {
  private String accountId;
  private String appId;
  private String stateExecutionId;
  private String workflowId;
  private String workflowExecutionId;
  private String serviceId;
  private String groupName;
  private String delegateTaskId;
  private Integer analysisMinute;

  @Inject private WingsPersistence wingsPersistence;
  @Inject private MetricDataAnalysisService metricDataAnalysisService;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    accountId = UUID.randomUUID().toString();
    appId = UUID.randomUUID().toString();
    stateExecutionId = UUID.randomUUID().toString();
    workflowId = UUID.randomUUID().toString();
    workflowExecutionId = UUID.randomUUID().toString();
    serviceId = UUID.randomUUID().toString();
    groupName = "groupName-";
    delegateTaskId = UUID.randomUUID().toString();
    analysisMinute = 10;
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

  @Test
  public void testSaveAnalysisRecordsML() {
    MetricAnalysisRecord mlAnalysisResponse = ExperimentalMetricAnalysisRecord.builder().build();

    Map<String, TimeSeriesMLTxnSummary> mlTxnSummaryByName = new ConcurrentHashMap<>();

    TimeSeriesMLTxnSummary summary = getSampleSummary();

    mlTxnSummaryByName.put("tx_name", summary);
    mlAnalysisResponse.setTransactions(mlTxnSummaryByName);

    metricDataAnalysisService.saveAnalysisRecordsML(StateType.DYNA_TRACE, accountId, appId, stateExecutionId,
        workflowExecutionId, workflowId, serviceId, groupName, analysisMinute, delegateTaskId, "-1",
        mlAnalysisResponse);

    List<ExperimentalMetricAnalysisRecord> resultList =
        metricDataAnalysisService.getExperimentalAnalysisRecordsByNaturalKey(
            appId, stateExecutionId, workflowExecutionId);

    assertNotNull(resultList);
    assertEquals(1, resultList.size());
    assertEquals(1, resultList.iterator().next().getTransactions().size());
    assertTrue(resultList.iterator().next().getTransactions().keySet().contains("tx_name"));
  }

  private TimeSeriesMLTxnSummary getSampleSummary() {
    TimeSeriesMLTxnSummary summary = new TimeSeriesMLTxnSummary();
    summary.setGroup_name(groupName);
    summary.setTxn_name("/todolist/inside/display.jsp:SERVICE_METHOD-DA487A489220E53D");
    summary.setTxn_tag("txn_tag");
    summary.setMetrics(getSummaryMetrics());
    return summary;
  }

  private Map<String, TimeSeriesMLMetricSummary> getSummaryMetrics() {
    Map<String, TimeSeriesMLMetricSummary> mlMetricSummaryByName = new ConcurrentHashMap<>();
    TimeSeriesMLMetricSummary metricSummary = new TimeSeriesMLMetricSummary();
    metricSummary.setMetric_name("serverSideFailureRate");
    metricSummary.setMetric_type("ERROR");

    TimeSeriesMLDataSummary control = new TimeSeriesMLDataSummary();
    control.setData(new ArrayList<>());
    control.setHost_names(new ArrayList<>());
    control.getHost_names().add("test_node");
    metricSummary.setControl(control);
    metricSummary.setResults(getHostSummary());

    mlMetricSummaryByName.put("serverSideFailureRate", metricSummary);
    return mlMetricSummaryByName;
  }

  private Map<String, TimeSeriesMLHostSummary> getHostSummary() {
    Map<String, TimeSeriesMLHostSummary> hostSummaryByName = new ConcurrentHashMap<>();
    TimeSeriesMLHostSummary hostSummary = new TimeSeriesMLHostSummary();
    List<Double> data = new ArrayList<>();
    data.add(1.0);
    hostSummary.setDistance(data);
    hostSummary.setControl_data(data);
    hostSummary.setTest_data(data);
    hostSummary.setControl_cuts(Collections.singletonList('q'));
    hostSummary.setTest_cuts(Collections.singletonList('q'));
    hostSummaryByName.put("test_host", hostSummary);
    return hostSummaryByName;
  }
}
