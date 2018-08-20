package software.wings.service;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.Lists;
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
import software.wings.resources.TimeSeriesResource;
import software.wings.service.impl.analysis.TSRequest;
import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;
import software.wings.service.impl.analysis.TimeSeriesMetricGroup.TimeSeriesMlAnalysisGroupInfo;
import software.wings.service.impl.analysis.TimeSeriesMlAnalysisType;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
  private String groupName;
  private String delegateTaskId;

  @Inject private NewRelicResource newRelicResource;
  @Inject private TimeSeriesResource timeSeriesResource;
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
    groupName = "groupName-";
    delegateTaskId = UUID.randomUUID().toString();
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testSaveMLAnalysis() throws IOException {
    InputStream is = getClass().getClassLoader().getResourceAsStream("verification/TimeSeriesNRAnalysisRecords.json");
    String jsonTxt = IOUtils.toString(is, Charset.defaultCharset());
    TimeSeriesMLAnalysisRecord record = JsonUtils.asObject(jsonTxt, TimeSeriesMLAnalysisRecord.class);
    int numOfGrpups = 1;
    for (int i = 0; i < numOfGrpups; i++) {
      timeSeriesResource.saveMLAnalysisRecords(accountId, appId, StateType.NEW_RELIC, stateExecutionId,
          workflowExecutionId, workflowId, serviceId, groupName + i, 0, null, null, record);
    }
    List<NewRelicMetricAnalysisRecord> analysisRecords =
        timeSeriesResource.getMetricsAnalysisAppdynamics(stateExecutionId, workflowExecutionId, accountId, appId)
            .getResource();
    assertEquals(numOfGrpups, analysisRecords.size());
    for (int i = 0; i < numOfGrpups; i++) {
      assertEquals(1, analysisRecords.get(i).getMetricAnalyses().size());
      assertEquals(
          "WebTransaction/Servlet/Register", analysisRecords.get(i).getMetricAnalyses().get(0).getMetricName());
      assertEquals(1, analysisRecords.get(0).getMetricAnalyses().get(i).getMetricValues().size());
      assertEquals(
          "requestsPerMinute", analysisRecords.get(i).getMetricAnalyses().get(0).getMetricValues().get(0).getName());
      assertEquals(groupName + i, analysisRecords.get(0).getGroupName());
    }
  }

  @Test
  public void testSaveAnalysis() throws IOException {
    int numOfGrpups = 5;
    for (int i = 0; i < numOfGrpups; i++) {
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
              .appId(appId)
              .stateExecutionId(stateExecutionId)
              .workflowExecutionId(workflowExecutionId)
              .message("1 high risk anomaly")
              .stateType(StateType.NEW_RELIC)
              .groupName(groupName + i)
              .build();

      metricDataAnalysisService.saveAnalysisRecords(newRelicMetricAnalysisRecord);
    }
    List<NewRelicMetricAnalysisRecord> analysisRecords =
        timeSeriesResource.getMetricsAnalysisAppdynamics(stateExecutionId, workflowExecutionId, accountId, appId)
            .getResource();
    assertEquals(numOfGrpups, analysisRecords.size());
    for (int i = 0; i < numOfGrpups; i++) {
      assertEquals(1, analysisRecords.get(i).getMetricAnalyses().size());
      assertEquals("index.jsp", analysisRecords.get(i).getMetricAnalyses().get(0).getMetricName());
      assertEquals(1, analysisRecords.get(i).getMetricAnalyses().get(0).getMetricValues().size());
      assertEquals(
          "requestsPerMinute", analysisRecords.get(i).getMetricAnalyses().get(0).getMetricValues().get(0).getName());
      assertEquals(groupName + i, analysisRecords.get(i).getGroupName());
    }
  }

  @Test
  public void testAnalysisSorting() throws IOException {
    Map<String, TimeSeriesMlAnalysisGroupInfo> groups = new HashMap<>();
    groups.put("b_group.with.dot",
        TimeSeriesMlAnalysisGroupInfo.builder()
            .groupName("b_group.with.dot")
            .mlAnalysisType(TimeSeriesMlAnalysisType.PREDICTIVE)
            .build());

    groups.put("fs_group",
        TimeSeriesMlAnalysisGroupInfo.builder()
            .groupName("fs_group")
            .mlAnalysisType(TimeSeriesMlAnalysisType.COMPARATIVE)
            .build());

    groups.put("a_group.with.dot",
        TimeSeriesMlAnalysisGroupInfo.builder()
            .groupName("a_group.with.dot")
            .mlAnalysisType(TimeSeriesMlAnalysisType.PREDICTIVE)
            .build());

    metricDataAnalysisService.saveMetricGroups(appId, StateType.NEW_RELIC, stateExecutionId, groups);
    NewRelicMetricAnalysisRecord newRelicMetricAnalysisRecord1 =
        NewRelicMetricAnalysisRecord.builder()
            .analysisMinute(0)
            .appId(appId)
            .stateExecutionId(stateExecutionId)
            .workflowExecutionId(workflowExecutionId)
            .message("No data available")
            .riskLevel(RiskLevel.NA)
            .stateType(StateType.NEW_RELIC)
            .groupName("b_group.with.dot")
            .mlAnalysisType(TimeSeriesMlAnalysisType.PREDICTIVE)
            .build();
    metricDataAnalysisService.saveAnalysisRecords(newRelicMetricAnalysisRecord1);

    NewRelicMetricAnalysisRecord newRelicMetricAnalysisRecord2 =
        NewRelicMetricAnalysisRecord.builder()
            .analysisMinute(0)
            .appId(appId)
            .stateExecutionId(stateExecutionId)
            .workflowExecutionId(workflowExecutionId)
            .message("No data available")
            .riskLevel(RiskLevel.NA)
            .stateType(StateType.NEW_RELIC)
            .groupName("fs_group")
            .mlAnalysisType(TimeSeriesMlAnalysisType.COMPARATIVE)
            .build();
    metricDataAnalysisService.saveAnalysisRecords(newRelicMetricAnalysisRecord2);

    NewRelicMetricAnalysisRecord newRelicMetricAnalysisRecord3 =
        NewRelicMetricAnalysisRecord.builder()
            .analysisMinute(0)
            .appId(appId)
            .stateExecutionId(stateExecutionId)
            .workflowExecutionId(workflowExecutionId)
            .message("No data available")
            .riskLevel(RiskLevel.NA)
            .stateType(StateType.NEW_RELIC)
            .groupName("a_group.with.dot")
            .mlAnalysisType(TimeSeriesMlAnalysisType.PREDICTIVE)
            .build();
    metricDataAnalysisService.saveAnalysisRecords(newRelicMetricAnalysisRecord3);

    List<NewRelicMetricAnalysisRecord> analysisRecords =
        timeSeriesResource.getMetricsAnalysisAppdynamics(stateExecutionId, workflowExecutionId, accountId, appId)
            .getResource();

    assertEquals(newRelicMetricAnalysisRecord2, analysisRecords.get(0));
    assertEquals(newRelicMetricAnalysisRecord3, analysisRecords.get(1));
    assertEquals(newRelicMetricAnalysisRecord1, analysisRecords.get(2));
  }

  @Test
  public void testNewRelicSorting() throws IOException {
    NewRelicMetricAnalysis indexAnalysis = getMetricAnalysis(
        "requestsPerMiniute", RiskLevel.LOW, 100, 2000, "appdex", RiskLevel.LOW, 100, 2000, "index.jsp");
    NewRelicMetricAnalysis accountAnalyis =
        getMetricAnalysis("requestsPerMiniute", RiskLevel.LOW, 100, 3000, "appdex", RiskLevel.LOW, 100, 423, "account");
    NewRelicMetricAnalysis loginAnalysis =
        getMetricAnalysis("requestsPerMiniute", RiskLevel.LOW, 100, 2500, "appdex", RiskLevel.LOW, 100, 8000, "login");

    NewRelicMetricAnalysisRecord newRelicMetricAnalysisRecord =
        NewRelicMetricAnalysisRecord.builder()
            .analysisMinute(0)
            .metricAnalyses(Lists.newArrayList(indexAnalysis, accountAnalyis, loginAnalysis))
            .appId(appId)
            .stateExecutionId(stateExecutionId)
            .workflowExecutionId(workflowExecutionId)
            .message("1 high risk anomaly")
            .stateType(StateType.NEW_RELIC)
            .build();

    metricDataAnalysisService.saveAnalysisRecords(newRelicMetricAnalysisRecord);
    List<NewRelicMetricAnalysisRecord> analysisRecords =
        timeSeriesResource.getMetricsAnalysisAppdynamics(stateExecutionId, workflowExecutionId, accountId, appId)
            .getResource();
    assertEquals(1, analysisRecords.size());

    NewRelicMetricAnalysisRecord analysisRecord = analysisRecords.get(0);
    assertEquals(3, analysisRecord.getMetricAnalyses().size());
    assertEquals("account", analysisRecord.getMetricAnalyses().get(0).getMetricName());
    assertEquals("index.jsp", analysisRecord.getMetricAnalyses().get(1).getMetricName());
    assertEquals("login", analysisRecord.getMetricAnalyses().get(2).getMetricName());
  }

  @Test
  public void testAppDSorting() throws IOException {
    NewRelicMetricAnalysis indexAnalysis =
        getMetricAnalysis("response95th", RiskLevel.LOW, 100, 2500, "stalls", RiskLevel.LOW, 100, 2000, "index.jsp");

    NewRelicMetricAnalysis accountAnalyis =
        getMetricAnalysis("response95th", RiskLevel.LOW, 100, 3000, "stalls", RiskLevel.LOW, 100, 423, "account");

    NewRelicMetricAnalysis loginAnalysis =
        getMetricAnalysis("response95th", RiskLevel.LOW, 100, 2500, "stalls", RiskLevel.LOW, 100, 8000, "login");

    NewRelicMetricAnalysisRecord newRelicMetricAnalysisRecord =
        NewRelicMetricAnalysisRecord.builder()
            .analysisMinute(0)
            .metricAnalyses(Lists.newArrayList(indexAnalysis, accountAnalyis, loginAnalysis))
            .appId(appId)
            .stateExecutionId(stateExecutionId)
            .workflowExecutionId(workflowExecutionId)
            .message("1 high risk anomaly")
            .stateType(StateType.APP_DYNAMICS)
            .build();

    metricDataAnalysisService.saveAnalysisRecords(newRelicMetricAnalysisRecord);
    List<NewRelicMetricAnalysisRecord> analysisRecords =
        timeSeriesResource.getMetricsAnalysisAppdynamics(stateExecutionId, workflowExecutionId, accountId, appId)
            .getResource();
    assertEquals(1, analysisRecords.size());

    NewRelicMetricAnalysisRecord analysisRecord = analysisRecords.get(0);
    assertEquals(3, analysisRecord.getMetricAnalyses().size());
    assertEquals("account", analysisRecord.getMetricAnalyses().get(0).getMetricName());
    assertEquals("index.jsp", analysisRecord.getMetricAnalyses().get(1).getMetricName());
    assertEquals("login", analysisRecord.getMetricAnalyses().get(2).getMetricName());
  }

  @Test
  public void testDynaTraceSorting() throws IOException {
    NewRelicMetricAnalysis indexAnalysis = getMetricAnalysis(
        "requestsPerMin", RiskLevel.LOW, 100, 2000, "serverSideError", RiskLevel.LOW, 100, 2000, "index.jsp");

    NewRelicMetricAnalysis accountAnalyis =
        getMetricAnalysis("requestsPerMin", RiskLevel.LOW, 100, 3000, "stalls", RiskLevel.LOW, 100, 423, "account");

    NewRelicMetricAnalysis loginAnalysis =
        getMetricAnalysis("requestsPerMin", RiskLevel.LOW, 100, 2500, "stalls", RiskLevel.LOW, 100, 8000, "login");

    NewRelicMetricAnalysisRecord newRelicMetricAnalysisRecord =
        NewRelicMetricAnalysisRecord.builder()
            .analysisMinute(0)
            .metricAnalyses(Lists.newArrayList(indexAnalysis, accountAnalyis, loginAnalysis))
            .appId(appId)
            .stateExecutionId(stateExecutionId)
            .workflowExecutionId(workflowExecutionId)
            .message("1 high risk anomaly")
            .stateType(StateType.DYNA_TRACE)
            .build();

    metricDataAnalysisService.saveAnalysisRecords(newRelicMetricAnalysisRecord);
    List<NewRelicMetricAnalysisRecord> analysisRecords =
        timeSeriesResource.getMetricsAnalysisAppdynamics(stateExecutionId, workflowExecutionId, accountId, appId)
            .getResource();
    assertEquals(1, analysisRecords.size());

    NewRelicMetricAnalysisRecord analysisRecord = analysisRecords.get(0);
    assertEquals(3, analysisRecord.getMetricAnalyses().size());
    assertEquals("account", analysisRecord.getMetricAnalyses().get(0).getMetricName());
    assertEquals("login", analysisRecord.getMetricAnalyses().get(1).getMetricName());
    assertEquals("index.jsp", analysisRecord.getMetricAnalyses().get(2).getMetricName());
  }

  @Test
  public void testDynatraceMetricNameReplacement() throws IOException {
    NewRelicMetricAnalysisValue metricAnalysisValue = NewRelicMetricAnalysisValue.builder()
                                                          .name("requestsPerMinute")
                                                          .riskLevel(RiskLevel.HIGH)
                                                          .controlValue(100)
                                                          .testValue(2000)
                                                          .build();
    NewRelicMetricAnalysis newRelicMetricAnalysis1 =
        NewRelicMetricAnalysis.builder()
            .metricName("startDelegateTask:SERVICE_METHOD-F9A70E1663C0B9A4")
            .metricValues(Collections.singletonList(metricAnalysisValue))
            .riskLevel(RiskLevel.MEDIUM)
            .build();
    NewRelicMetricAnalysis newRelicMetricAnalysis2 = NewRelicMetricAnalysis.builder()
                                                         .metricName("index.jsp")
                                                         .metricValues(Collections.singletonList(metricAnalysisValue))
                                                         .riskLevel(RiskLevel.MEDIUM)
                                                         .build();
    NewRelicMetricAnalysisRecord newRelicMetricAnalysisRecord =
        NewRelicMetricAnalysisRecord.builder()
            .analysisMinute(0)
            .metricAnalyses(Lists.newArrayList(newRelicMetricAnalysis1, newRelicMetricAnalysis2))
            .appId(appId)
            .stateExecutionId(stateExecutionId)
            .workflowExecutionId(workflowExecutionId)
            .message("1 high risk anomaly")
            .stateType(StateType.DYNA_TRACE)
            .build();

    metricDataAnalysisService.saveAnalysisRecords(newRelicMetricAnalysisRecord);
    List<NewRelicMetricAnalysisRecord> analysisRecords =
        timeSeriesResource.getMetricsAnalysisAppdynamics(stateExecutionId, workflowExecutionId, accountId, appId)
            .getResource();
    assertEquals(1, analysisRecords.size());

    NewRelicMetricAnalysisRecord analysisRecord = analysisRecords.get(0);
    List<NewRelicMetricAnalysis> metricAnalyses = analysisRecord.getMetricAnalyses();
    assertEquals(2, metricAnalyses.size());
    assertEquals("index.jsp", metricAnalyses.get(0).getMetricName());
    assertEquals("index.jsp", metricAnalyses.get(0).getDisplayName());
    assertEquals("index.jsp", metricAnalyses.get(0).getFullMetricName());

    assertEquals("startDelegateTask:SERVICE_METHOD-F9A70E1663C0B9A4", metricAnalyses.get(1).getMetricName());
    assertEquals("startDelegateTask", metricAnalyses.get(1).getDisplayName());
    assertEquals("startDelegateTask (SERVICE_METHOD-F9A70E1663C0B9A4)", metricAnalyses.get(1).getFullMetricName());
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
      record.setAppId(appId);
      record.setWorkflowId(workflowId);
      record.setWorkflowExecutionId(workflowExecutionId);
      record.setStateExecutionId(stateExecutionId);
      record.setServiceId(serviceId);
      record.setGroupName(groupName);
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
    List<NewRelicMetricDataRecord> results =
        timeSeriesResource
            .getMetricData(accountId, appId, StateType.NEW_RELIC, workflowExecutionId, groupName, true,
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

  private NewRelicMetricAnalysis getMetricAnalysis(String metricAnalysisValueName1, RiskLevel riskLevel1,
      double controlValue1, double testValue1, String metricAnalysisValueName2, RiskLevel riskLevel2,
      double controlValue2, double testValue2, String metricName) {
    NewRelicMetricAnalysisValue requestsPerMinute = NewRelicMetricAnalysisValue.builder()
                                                        .name(metricAnalysisValueName1)
                                                        .riskLevel(riskLevel1)
                                                        .controlValue(controlValue1)
                                                        .testValue(testValue1)
                                                        .build();
    NewRelicMetricAnalysisValue appdex = NewRelicMetricAnalysisValue.builder()
                                             .name(metricAnalysisValueName2)
                                             .riskLevel(riskLevel2)
                                             .controlValue(controlValue2)
                                             .testValue(testValue2)
                                             .build();
    return NewRelicMetricAnalysis.builder()
        .metricName(metricName)
        .metricValues(Lists.newArrayList(requestsPerMinute, appdex))
        .riskLevel(RiskLevel.LOW)
        .build();
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
