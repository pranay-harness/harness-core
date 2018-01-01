package software.wings.service;

import static org.junit.Assert.assertEquals;

import com.google.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import software.wings.WingsBaseTest;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.metrics.RiskLevel;
import software.wings.resources.NewRelicResource;
import software.wings.service.impl.analysis.TSRequest;
import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricAnalysis;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricAnalysisValue;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.utils.JsonUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by sriram_parthasarathy on 10/16/17.
 */
@RunWith(MockitoJUnitRunner.class)
public class TimeSeriesMLAnalysisTest extends WingsBaseTest {
  private String accountId;
  private String appId;
  private String stateExecutionId;
  private String workflowId;
  private String workflowExecutionId;
  private String serviceId;
  private String delegateTaskId;

  @Inject private NewRelicResource newRelicResource;
  @Mock private DelegateProxyFactory delegateProxyFactory;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private MetricDataAnalysisService metricDataAnalysisService;

  @Before
  public void setup() throws IOException {
    accountId = UUID.randomUUID().toString();
    appId = UUID.randomUUID().toString();
    stateExecutionId = UUID.randomUUID().toString();
    workflowId = UUID.randomUUID().toString();
    workflowExecutionId = UUID.randomUUID().toString();
    serviceId = UUID.randomUUID().toString();
    delegateTaskId = UUID.randomUUID().toString();
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testSaveMLAnalysis() throws IOException {
    InputStream is = getClass().getClassLoader().getResourceAsStream("verification/TimeSeriesNRAnalysisRecords.json");
    String jsonTxt = IOUtils.toString(is, Charset.defaultCharset());
    TimeSeriesMLAnalysisRecord record = JsonUtils.asObject(jsonTxt, TimeSeriesMLAnalysisRecord.class);
    newRelicResource.saveMLAnalysisRecords(
        accountId, appId, stateExecutionId, workflowExecutionId, workflowId, 0, record);
    NewRelicMetricAnalysisRecord analysisRecord =
        newRelicResource.getMetricsAnalysis(stateExecutionId, workflowExecutionId, accountId).getResource();
    assertEquals(analysisRecord.getMetricAnalyses().size(), 1);
    assertEquals(analysisRecord.getMetricAnalyses().get(0).getMetricName(), "WebTransaction/Servlet/Register");
    assertEquals(analysisRecord.getMetricAnalyses().get(0).getMetricValues().size(), 1);
    assertEquals(analysisRecord.getMetricAnalyses().get(0).getMetricValues().get(0).getName(), "requestsPerMinute");
  }

  @Test
  public void testSaveAnalysis() throws IOException {
    NewRelicMetricAnalysisValue metricAnalysisValue = NewRelicMetricAnalysisValue.builder()
                                                          .name("requestsPerMinute")
                                                          .riskLevel(RiskLevel.HIGH)
                                                          .controlValue(100)
                                                          .testValue(2000)
                                                          .build();
    NewRelicMetricAnalysis newRelicMetricAnalysis = NewRelicMetricAnalysis.builder()
                                                        .metricName("index.jsp")
                                                        .metricValues(Collections.singletonList(metricAnalysisValue))
                                                        .riskLevel(RiskLevel.MEDIUM)
                                                        .build();
    NewRelicMetricAnalysisRecord newRelicMetricAnalysisRecord =
        NewRelicMetricAnalysisRecord.builder()
            .analysisMinute(0)
            .metricAnalyses(Collections.singletonList(newRelicMetricAnalysis))
            .applicationId(appId)
            .stateExecutionId(stateExecutionId)
            .workflowExecutionId(workflowExecutionId)
            .message("1 high risk anomaly")
            .stateType(StateType.NEW_RELIC)
            .build();

    metricDataAnalysisService.saveAnalysisRecords(newRelicMetricAnalysisRecord);
    NewRelicMetricAnalysisRecord analysisRecord =
        newRelicResource.getMetricsAnalysis(stateExecutionId, workflowExecutionId, accountId).getResource();
    assertEquals(analysisRecord.getMetricAnalyses().size(), 1);
    assertEquals(analysisRecord.getMetricAnalyses().get(0).getMetricName(), "index.jsp");
    assertEquals(analysisRecord.getMetricAnalyses().get(0).getMetricValues().size(), 1);
    assertEquals(analysisRecord.getMetricAnalyses().get(0).getMetricValues().get(0).getName(), "requestsPerMinute");
  }

  private List<NewRelicMetricDataRecord> loadMetrics(String fileName) throws IOException {
    InputStream is = getClass().getClassLoader().getResourceAsStream(fileName);
    String jsonTxt = IOUtils.toString(is, Charset.defaultCharset());
    return JsonUtils.asList(jsonTxt, new TypeReference<List<NewRelicMetricDataRecord>>() {});
  }

  private Set<String> setIdsGetNodes(
      List<NewRelicMetricDataRecord> records, String workflowExecutionId, String stateExecutionId) {
    Set<String> nodes = new HashSet<>();
    for (Iterator<NewRelicMetricDataRecord> it = records.iterator(); it.hasNext();) {
      NewRelicMetricDataRecord record = it.next();
      if (record.getDataCollectionMinute() != 0) {
        it.remove();
      }
      record.setStateType(StateType.NEW_RELIC);
      record.setApplicationId(appId);
      record.setWorkflowId(workflowId);
      record.setWorkflowExecutionId(workflowExecutionId);
      record.setStateExecutionId(stateExecutionId);
      record.setServiceId(serviceId);
      record.setTimeStamp(record.getDataCollectionMinute());
      nodes.add(record.getHost());
    }
    return nodes;
  }

  @Test
  public void testSaveMetricRecords() throws IOException {
    List<NewRelicMetricDataRecord> controlRecords = loadMetrics("./verification/TimeSeriesNRControlInput.json");
    Set<String> nodes = setIdsGetNodes(controlRecords, workflowExecutionId, stateExecutionId);
    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setUuid(stateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    stateExecutionInstance.setAppId(appId);
    wingsPersistence.saveIgnoringDuplicateKeys(Collections.singletonList(stateExecutionInstance));
    newRelicResource.saveMetricData(accountId, appId, stateExecutionId, delegateTaskId, controlRecords);
    List<NewRelicMetricDataRecord> results = newRelicResource
                                                 .getMetricData(accountId, workflowExecutionId, true,
                                                     TSRequest.builder()
                                                         .applicationId(appId)
                                                         .workflowId(workflowId)
                                                         .workflowExecutionId(workflowExecutionId)
                                                         .stateExecutionId(stateExecutionId)
                                                         .serviceId(serviceId)
                                                         .analysisMinute(0)
                                                         .nodes(nodes)
                                                         .build())
                                                 .getResource();

    assertEquals(results.size(), controlRecords.size());
  }

  //  @Test
  //  public void testAnalysis() throws IOException {
  //    List<NewRelicMetricDataRecord> controlRecords = loadMetrics("./verification/TimeSeriesNRControlInput.json");
  //    List<NewRelicMetricDataRecord> testRecords = loadMetrics("./verification/TimeSeriesNRTestInput.json");
  //    UUID workFlowExecutionId = UUID.randomUUID();
  //    UUID stateExecutionId = UUID.randomUUID();
  //    Set<String> controlNodes = setIdsGetNodes(controlRecords, workflowExecutionId, stateExecutionId);
  //    Set<String> testNodes = setIdsGetNodes(controlRecords, workflowExecutionId, stateExecutionId);
  //    newRelicResource.saveMetricData(accountId, appId, controlRecords);
  //    newRelicResource.saveMetricData(accountId, appId, controlRecords);
  //
  //    JobExecutionContext context = Mockito.mock(JobExecutionContext.class);
  //    AnalysisContext analysisContext = AnalysisContext.builder().accountId(accountId)
  //        .appId(appId)
  //        .accountId(accountId)
  //        .tolerance(1)
  //        .smooth_window(3)
  //        .comparisonStrategy(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS)
  //        .serviceId(serviceId)
  //        .stateExecutionId(stateExecutionId)
  //        .stateType(StateType.NEW_RELIC)
  //        .stateBaseUrl("newrelic")
  //        .workflowExecutionId(workflowExecutionId)
  //        .appPort(9090)
  //        .isSSL(true)
  //        .authToken(AbstractAnalysisState.generateAuthToken("nhUmut2NMcUnsR01OgOz0e51MZ51AqUwrOATJ3fJ"))
  //    MetricAnalysisJob.MetricAnalysisGenerator metricAnalysisGenerator = new
  //    MetricAnalysisJob.MetricAnalysisGenerator(context)
  //  }
}
