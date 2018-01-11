package software.wings.integration;

import static java.util.Arrays.asList;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder.aWorkflowExecution;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;

import org.assertj.core.util.Sets;
import org.junit.Before;
import org.junit.Test;
import org.mongodb.morphia.query.Query;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import software.wings.beans.CountsByStatuses;
import software.wings.beans.RestResponse;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.scheduler.LogAnalysisManagerJob;
import software.wings.scheduler.LogAnalysisManagerJob.LogAnalysisTask;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.AnalysisServiceImpl;
import software.wings.service.impl.analysis.LogClusterContext;
import software.wings.service.impl.analysis.LogDataRecord;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.analysis.LogMLAnalysisRecord;
import software.wings.service.impl.analysis.LogMLAnalysisSummary;
import software.wings.service.impl.analysis.LogMLClusterGenerator;
import software.wings.service.impl.analysis.LogRequest;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.sm.states.AbstractAnalysisState;
import software.wings.sm.states.AbstractLogAnalysisState;
import software.wings.utils.JsonUtils;
import software.wings.waitnotify.WaitNotifyEngine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

/**
 * Created by rsingh on 8/17/17.
 */
public class ElkIntegrationTest extends BaseIntegrationTest {
  private Set<String> hosts = new HashSet<>();
  @Inject private AnalysisService analysisService;
  @Inject private LogAnalysisManagerJob logAnalysisManagerJob;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private DelegateService delegateService;

  private String appId;
  private String stateExecutionId;
  private String workflowId;
  private String workflowExecutionId;
  private String serviceId;
  private String delegateTaskId;

  @Before
  public void setUp() throws Exception {
    loginAdminUser();
    deleteAllDocuments(asList(LogDataRecord.class));
    deleteAllDocuments(asList(WorkflowExecution.class));
    deleteAllDocuments(asList(Workflow.class));
    hosts.clear();
    hosts.add("ip-172-31-2-144");
    hosts.add("ip-172-31-4-253");
    hosts.add("ip-172-31-12-51");
    hosts.add("ip-172-31-12-78");
    hosts.add("ip-172-31-15-177");
    appId = UUID.randomUUID().toString();
    stateExecutionId = UUID.randomUUID().toString();
    workflowId = UUID.randomUUID().toString();
    workflowExecutionId = UUID.randomUUID().toString();
    serviceId = UUID.randomUUID().toString();
    delegateTaskId = UUID.randomUUID().toString();
  }

  @Test
  public void testFirstLevelClustering() throws Exception {
    for (String host : hosts) {
      File file = new File(getClass().getClassLoader().getResource("./elk/" + host + ".json").getFile());

      List<LogDataRecord> logDataRecords = readLogDataRecordsFromFile(file);
      Map<Integer, List<LogElement>> recordsByMinute = splitRecordsByMinute(logDataRecords);

      final String stateExecutionId = logDataRecords.get(0).getStateExecutionId();
      final String workflowId = logDataRecords.get(0).getWorkflowId();
      final String workflowExecutionId = logDataRecords.get(0).getWorkflowExecutionId();
      final String applicationId = logDataRecords.get(0).getApplicationId();
      final String serviceId = logDataRecords.get(0).getServiceId();
      final String query = ".*exception.*";

      StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
      stateExecutionInstance.setUuid(stateExecutionId);
      stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
      stateExecutionInstance.setAppId(applicationId);
      wingsPersistence.saveIgnoringDuplicateKeys(Collections.singletonList(stateExecutionInstance));

      wingsPersistence.saveIgnoringDuplicateKeys(logDataRecords);

      for (int logCollectionMinute : recordsByMinute.keySet()) {
        final LogClusterContext logClusterContext =
            LogClusterContext.builder()
                .accountId(accountId)
                .appId(applicationId)
                .workflowExecutionId(workflowExecutionId)
                .workflowId(workflowId)
                .stateExecutionId(stateExecutionId)
                .serviceId(serviceId)
                .controlNodes(Sets.newHashSet(Collections.singletonList(host)))
                .testNodes(Sets.newHashSet(Collections.singletonList(host)))
                .queries(Sets.newHashSet(Collections.singletonList(query)))
                .isSSL(true)
                .appPort(9090)
                .stateType(StateType.ELK)
                .stateBaseUrl(LogAnalysisResource.ELK_RESOURCE_BASE_URL)
                .authToken(AbstractAnalysisState.generateAuthToken("nhUmut2NMcUnsR01OgOz0e51MZ51AqUwrOATJ3fJ"))
                .build();

        new LogMLClusterGenerator(logClusterContext, ClusterLevel.L0, ClusterLevel.L1,
            new LogRequest(query, applicationId, stateExecutionId, workflowId, serviceId,
                Sets.newHashSet(Collections.singletonList(host)), logCollectionMinute))
            .run();
      }

      for (int logCollectionMinute : recordsByMinute.keySet()) {
        final LogRequest logRequest = new LogRequest(query, applicationId, stateExecutionId, workflowId, serviceId,
            Collections.singleton(host), logCollectionMinute);

        WebTarget getTarget = client.target(API_BASE + "/" + LogAnalysisResource.ELK_RESOURCE_BASE_URL
            + LogAnalysisResource.ANALYSIS_STATE_GET_LOG_URL + "?accountId=" + accountId + "&clusterLevel="
            + ClusterLevel.L1.name() + "&compareCurrent=true&workflowExecutionId=" + workflowExecutionId);
        RestResponse<List<LogDataRecord>> restResponse = getRequestBuilderWithAuthHeader(getTarget).post(
            entity(logRequest, APPLICATION_JSON), new GenericType<RestResponse<List<LogDataRecord>>>() {});
        assertEquals(
            "failed for " + host + " for minute " + logCollectionMinute, 15, restResponse.getResource().size());
      }
    }
  }

  private Map<Integer, List<LogElement>> splitRecordsByMinute(List<LogDataRecord> logDataRecords) {
    final Map<Integer, List<LogElement>> rv = new HashMap<>();
    for (LogDataRecord logDataRecord : logDataRecords) {
      if (!rv.containsKey(logDataRecord.getLogCollectionMinute())) {
        rv.put(logDataRecord.getLogCollectionMinute(), new ArrayList<>());
      }

      rv.get(logDataRecord.getLogCollectionMinute())
          .add(new LogElement(logDataRecord.getQuery(), logDataRecord.getClusterLabel(), logDataRecord.getHost(),
              logDataRecord.getTimeStamp(), logDataRecord.getCount(), logDataRecord.getLogMessage(),
              logDataRecord.getLogCollectionMinute()));
    }
    return rv;
  }

  private List<LogDataRecord> readLogDataRecordsFromFile(File file) throws FileNotFoundException {
    final Gson gson = new Gson();
    BufferedReader br = new BufferedReader(new FileReader(file));
    Type type = new TypeToken<List<LogDataRecord>>() {}.getType();
    return gson.fromJson(br, type);
  }

  // TODO Disabled test. Enable when purge is revisited
  public void testPurge() throws Exception {
    final String sameApplicationId = "some-application";
    final String sameServiceId = "some-service";
    int numOfWorkFlows = 3;
    int numOfExecutionPerWorkFlow = 5;
    int numOfMinutes = 10;
    int numOfLogs = 12;

    Map<String, String> workFlowToExecution = new HashMap<>();
    Map<String, String> workFlowToStateExecution = new HashMap<>();
    int totalRecordsInserted = 0;
    for (int workflowNum = 0; workflowNum < numOfWorkFlows; workflowNum++) {
      Workflow workflow = aWorkflow().withAppId(sameApplicationId).withName("workflow-" + workflowNum).build();
      String workFlowId = wingsPersistence.save(workflow);
      for (int executionNum = 0; executionNum < numOfExecutionPerWorkFlow; executionNum++) {
        WorkflowExecution workflowExecution = aWorkflowExecution()
                                                  .withWorkflowId(workFlowId)
                                                  .withAppId(sameApplicationId)
                                                  .withName(workFlowId + "-execution-" + executionNum)
                                                  .withStatus(ExecutionStatus.SUCCESS)
                                                  .build();
        String workFlowExecutionId = wingsPersistence.save(workflowExecution);
        workFlowToExecution.put(workFlowId, workFlowExecutionId);
        final String stateExecutionId = workFlowExecutionId + "-state-execution-" + executionNum;
        workFlowToStateExecution.put(workFlowId, stateExecutionId);
        for (StateType stateType : AnalysisServiceImpl.logAnalysisStates) {
          for (String host : hosts) {
            Map<Integer, List<LogElement>> recordsByMinute = generateLogElements(host, numOfMinutes, numOfLogs);
            WebTarget target = client.target(API_BASE + "/" + AbstractLogAnalysisState.getStateBaseUrl(stateType)
                + LogAnalysisResource.ANALYSIS_STATE_SAVE_LOG_URL + "?accountId=" + accountId
                + "&clusterLevel=" + ClusterLevel.L2.name() + "&stateExecutionId=" + stateExecutionId
                + "&workflowId=" + workFlowId + "&workflowExecutionId=" + workFlowExecutionId
                + "&appId=" + sameApplicationId + "&serviceId=" + sameServiceId);

            for (Entry<Integer, List<LogElement>> entry : recordsByMinute.entrySet()) {
              totalRecordsInserted += entry.getValue().size();
              RestResponse<Boolean> restResponse = getDelegateRequestBuilderWithAuthHeader(target).post(
                  entity(entry.getValue(), APPLICATION_JSON), new GenericType<RestResponse<Boolean>>() {});
              assertTrue(restResponse.getResource());
            }
          }
        }
        System.out.println("done for workFlow: " + workFlowId + " execution: " + workFlowExecutionId);
      }
    }

    Query<LogDataRecord> logDataRecordQuery = wingsPersistence.createQuery(LogDataRecord.class);
    assertEquals(totalRecordsInserted, logDataRecordQuery.asList().size());
    analysisService.purgeLogs();
    logDataRecordQuery = wingsPersistence.createQuery(LogDataRecord.class);
    List<LogDataRecord> logDataRecords = logDataRecordQuery.asList();
    assertEquals(
        numOfMinutes * numOfLogs * hosts.size() * AnalysisServiceImpl.logAnalysisStates.length * numOfWorkFlows,
        logDataRecords.size());

    for (LogDataRecord logDataRecord : logDataRecords) {
      assertEquals(workFlowToExecution.get(logDataRecord.getWorkflowId()), logDataRecord.getWorkflowExecutionId());
      assertEquals(workFlowToStateExecution.get(logDataRecord.getWorkflowId()), logDataRecord.getStateExecutionId());
    }
  }

  private Map<Integer, List<LogElement>> generateLogElements(String host, int numOfMinutes, int numOfLogs) {
    long timeStamp = System.currentTimeMillis();
    Map<Integer, List<LogElement>> rv = new HashMap<>();
    for (int i = 0; i < numOfMinutes; i++) {
      rv.put(i, new ArrayList<>());

      long timeStampMin = timeStamp + TimeUnit.MINUTES.toMillis(i);
      for (int j = 0; j < numOfLogs; j++) {
        rv.get(i).add(new LogElement(
            "*exception", UUID.randomUUID().toString(), host, timeStampMin, 32, UUID.randomUUID().toString(), i));
      }
    }
    return rv;
  }

  @Test
  public void controlButNoTestData() throws IOException, JobExecutionException, InterruptedException {
    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    String prevStateExecutionId = UUID.randomUUID().toString();
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setUuid(prevStateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);

    WorkflowExecution workflowExecution =
        aWorkflowExecution()
            .withWorkflowId(workflowId)
            .withAppId(appId)
            .withName(workflowId + "-execution-" + 0)
            .withStatus(ExecutionStatus.SUCCESS)
            .withBreakdown(CountsByStatuses.Builder.aCountsByStatuses().withSuccess(1).build())
            .build();
    String workFlowExecutionId = wingsPersistence.save(workflowExecution);

    List<LogElement> logElements = new ArrayList<>();

    final String query = UUID.randomUUID().toString();
    final String host = UUID.randomUUID().toString();
    final int logCollectionMinute = 0;
    LogElement splunkHeartBeatElement = new LogElement();
    splunkHeartBeatElement.setQuery(query);
    splunkHeartBeatElement.setClusterLabel("-3");
    splunkHeartBeatElement.setHost(host);
    splunkHeartBeatElement.setCount(0);
    splunkHeartBeatElement.setLogMessage("");
    splunkHeartBeatElement.setTimeStamp(0);
    splunkHeartBeatElement.setLogCollectionMinute(logCollectionMinute);

    logElements.add(splunkHeartBeatElement);

    LogElement logElement = new LogElement(query, "0", host, 0, 0, "Hello World", logCollectionMinute);
    logElements.add(logElement);

    analysisService.saveLogData(StateType.SPLUNKV2, accountId, appId, prevStateExecutionId, workflowId,
        workFlowExecutionId, serviceId, ClusterLevel.L2, delegateTaskId, logElements);

    stateExecutionInstance.setStatus(ExecutionStatus.SUCCESS);
    wingsPersistence.save(stateExecutionInstance);

    stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setUuid(stateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);

    workflowExecution = aWorkflowExecution()
                            .withUuid(workflowExecutionId)
                            .withWorkflowId(workflowId)
                            .withAppId(appId)
                            .withName(workflowId + "-execution-" + 0)
                            .withStatus(ExecutionStatus.RUNNING)
                            .build();
    wingsPersistence.save(workflowExecution);

    logElements = new ArrayList<>();

    splunkHeartBeatElement = new LogElement();
    splunkHeartBeatElement.setQuery(query);
    splunkHeartBeatElement.setClusterLabel("-3");
    splunkHeartBeatElement.setHost(host);
    splunkHeartBeatElement.setCount(0);
    splunkHeartBeatElement.setLogMessage("");
    splunkHeartBeatElement.setTimeStamp(0);
    splunkHeartBeatElement.setLogCollectionMinute(logCollectionMinute);

    logElements.add(splunkHeartBeatElement);

    analysisService.saveLogData(StateType.SPLUNKV2, accountId, appId, stateExecutionId, workflowId, workflowExecutionId,
        serviceId, ClusterLevel.L1, delegateTaskId, logElements);

    AnalysisContext analysisContext =
        AnalysisContext.builder()
            .accountId(accountId)
            .appId(appId)
            .workflowId(workflowId)
            .workflowExecutionId(workflowExecutionId)
            .stateExecutionId(stateExecutionId)
            .serviceId(serviceId)
            .controlNodes(com.google.common.collect.Sets.newHashSet(host))
            .testNodes(com.google.common.collect.Sets.newHashSet(host))
            .queries(com.google.common.collect.Sets.newHashSet(query.split(",")))
            .isSSL(true)
            .appPort(9090)
            .comparisonStrategy(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS)
            .timeDuration(1)
            .stateType(StateType.SPLUNKV2)
            .stateBaseUrl(LogAnalysisResource.SPLUNK_RESOURCE_BASE_URL)
            .authToken(AbstractAnalysisState.generateAuthToken("nhUmut2NMcUnsR01OgOz0e51MZ51AqUwrOATJ3fJ"))
            .correlationId(UUID.randomUUID().toString())
            .build();
    JobExecutionContext jobExecutionContext = mock(JobExecutionContext.class);
    JobDataMap jobDataMap = mock(JobDataMap.class);
    when(jobDataMap.getLong("timestamp")).thenReturn(System.currentTimeMillis());
    when(jobDataMap.getString("jobParams")).thenReturn(JsonUtils.asJson(analysisContext));
    when(jobDataMap.getString("delegateTaskId")).thenReturn(UUID.randomUUID().toString());
    when(jobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);
    when(jobExecutionContext.getScheduler()).thenReturn(mock(Scheduler.class));
    when(jobExecutionContext.getJobDetail()).thenReturn(mock(JobDetail.class));

    new LogAnalysisTask(
        analysisService, waitNotifyEngine, delegateService, analysisContext, jobExecutionContext, delegateTaskId)
        .run();
    LogMLAnalysisSummary logMLAnalysisSummary =
        analysisService.getAnalysisSummary(stateExecutionId, appId, StateType.SPLUNKV2);
    assertEquals(logMLAnalysisSummary.getControlClusters().size(), 1);
    assertEquals(logMLAnalysisSummary.getTestClusters().size(), 0);
    assertEquals(logMLAnalysisSummary.getAnalysisSummaryMessage(),
        "No new data for the given queries. Showing baseline data if any.");
  }

  @Test
  public void testButNoControlData() throws IOException, JobExecutionException, InterruptedException {
    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    String prevStateExecutionId = UUID.randomUUID().toString();
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setUuid(prevStateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);

    WorkflowExecution workflowExecution =
        aWorkflowExecution()
            .withWorkflowId(workflowId)
            .withAppId(appId)
            .withName(workflowId + "-execution-" + 0)
            .withStatus(ExecutionStatus.SUCCESS)
            .withBreakdown(CountsByStatuses.Builder.aCountsByStatuses().withSuccess(1).build())
            .build();
    String workFlowExecutionId = wingsPersistence.save(workflowExecution);

    List<LogElement> logElements = new ArrayList<>();

    final String query = UUID.randomUUID().toString();
    final String host = UUID.randomUUID().toString();
    final int logCollectionMinute = 0;
    LogElement splunkHeartBeatElement = new LogElement();
    splunkHeartBeatElement.setQuery(query);
    splunkHeartBeatElement.setClusterLabel("-3");
    splunkHeartBeatElement.setHost(host);
    splunkHeartBeatElement.setCount(0);
    splunkHeartBeatElement.setLogMessage("");
    splunkHeartBeatElement.setTimeStamp(0);
    splunkHeartBeatElement.setLogCollectionMinute(logCollectionMinute);

    logElements.add(splunkHeartBeatElement);

    analysisService.saveLogData(StateType.SUMO, accountId, appId, prevStateExecutionId, workflowId, workFlowExecutionId,
        serviceId, ClusterLevel.L2, delegateTaskId, logElements);

    stateExecutionInstance.setStatus(ExecutionStatus.SUCCESS);
    wingsPersistence.save(stateExecutionInstance);

    stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setUuid(stateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);

    workflowExecution = aWorkflowExecution()
                            .withUuid(workflowExecutionId)
                            .withWorkflowId(workflowId)
                            .withAppId(appId)
                            .withName(workflowId + "-execution-" + 0)
                            .withStatus(ExecutionStatus.RUNNING)
                            .build();
    wingsPersistence.save(workflowExecution);

    logElements = new ArrayList<>();

    splunkHeartBeatElement = new LogElement();
    splunkHeartBeatElement.setQuery(query);
    splunkHeartBeatElement.setClusterLabel("-3");
    splunkHeartBeatElement.setHost(host);
    splunkHeartBeatElement.setCount(0);
    splunkHeartBeatElement.setLogMessage("");
    splunkHeartBeatElement.setTimeStamp(0);
    splunkHeartBeatElement.setLogCollectionMinute(logCollectionMinute);

    LogElement logElement = new LogElement(query, "0", host, 0, 0, "Hello World", logCollectionMinute);
    logElements.add(logElement);

    logElements.add(splunkHeartBeatElement);

    analysisService.saveLogData(StateType.SUMO, accountId, appId, stateExecutionId, workflowId, workflowExecutionId,
        serviceId, ClusterLevel.L1, delegateTaskId, logElements);

    AnalysisContext analysisContext =
        AnalysisContext.builder()
            .accountId(accountId)
            .appId(appId)
            .workflowId(workflowId)
            .workflowExecutionId(workflowExecutionId)
            .stateExecutionId(stateExecutionId)
            .serviceId(serviceId)
            .controlNodes(com.google.common.collect.Sets.newHashSet(host))
            .testNodes(com.google.common.collect.Sets.newHashSet(host))
            .queries(com.google.common.collect.Sets.newHashSet(query.split(",")))
            .isSSL(true)
            .appPort(9090)
            .comparisonStrategy(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS)
            .timeDuration(1)
            .stateType(StateType.SUMO)
            .stateBaseUrl(LogAnalysisResource.SUMO_RESOURCE_BASE_URL)
            .authToken(AbstractAnalysisState.generateAuthToken("nhUmut2NMcUnsR01OgOz0e51MZ51AqUwrOATJ3fJ"))
            .correlationId(UUID.randomUUID().toString())
            .build();
    JobExecutionContext jobExecutionContext = mock(JobExecutionContext.class);
    JobDataMap jobDataMap = mock(JobDataMap.class);
    when(jobDataMap.getLong("timestamp")).thenReturn(System.currentTimeMillis());
    when(jobDataMap.getString("jobParams")).thenReturn(JsonUtils.asJson(analysisContext));
    when(jobDataMap.getString("delegateTaskId")).thenReturn(UUID.randomUUID().toString());
    when(jobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);
    when(jobExecutionContext.getScheduler()).thenReturn(mock(Scheduler.class));
    when(jobExecutionContext.getJobDetail()).thenReturn(mock(JobDetail.class));

    new LogAnalysisTask(
        analysisService, waitNotifyEngine, delegateService, analysisContext, jobExecutionContext, delegateTaskId)
        .run();
    LogMLAnalysisSummary logMLAnalysisSummary =
        analysisService.getAnalysisSummary(stateExecutionId, appId, StateType.SUMO);
    assertEquals(logMLAnalysisSummary.getAnalysisSummaryMessage(),
        "No baseline data for the given queries. This will be baseline for the next run.");
    LogMLAnalysisRecord logAnalysisRecord =
        analysisService.getLogAnalysisRecords(appId, stateExecutionId, query, StateType.SUMO, logCollectionMinute);
    assertFalse(logAnalysisRecord.isBaseLineCreated());
    assertEquals(logAnalysisRecord.getControl_clusters().size(), 1);
    assertNull(logAnalysisRecord.getTest_clusters());
    assertNull(logAnalysisRecord.getTest_events());
  }

  @Test
  public void noControlandTestData() throws IOException, JobExecutionException, InterruptedException {
    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    String prevStateExecutionId = UUID.randomUUID().toString();
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setUuid(prevStateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);

    WorkflowExecution workflowExecution =
        aWorkflowExecution()
            .withWorkflowId(workflowId)
            .withAppId(appId)
            .withName(workflowId + "-execution-" + 0)
            .withStatus(ExecutionStatus.SUCCESS)
            .withBreakdown(CountsByStatuses.Builder.aCountsByStatuses().withSuccess(1).build())
            .build();
    String workFlowExecutionId = wingsPersistence.save(workflowExecution);

    List<LogElement> logElements = new ArrayList<>();

    final String query = UUID.randomUUID().toString();
    final String host = UUID.randomUUID().toString();
    final int logCollectionMinute = 0;
    LogElement elkBeatElement = new LogElement();
    elkBeatElement.setQuery(query);
    elkBeatElement.setClusterLabel("-3");
    elkBeatElement.setHost(host);
    elkBeatElement.setCount(0);
    elkBeatElement.setLogMessage("");
    elkBeatElement.setTimeStamp(0);
    elkBeatElement.setLogCollectionMinute(logCollectionMinute);

    logElements.add(elkBeatElement);

    analysisService.saveLogData(StateType.ELK, accountId, appId, prevStateExecutionId, workflowId, workFlowExecutionId,
        serviceId, ClusterLevel.L2, delegateTaskId, logElements);

    stateExecutionInstance.setStatus(ExecutionStatus.SUCCESS);
    wingsPersistence.save(stateExecutionInstance);

    stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setUuid(stateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);

    workflowExecution = aWorkflowExecution()
                            .withUuid(workflowExecutionId)
                            .withWorkflowId(workflowId)
                            .withAppId(appId)
                            .withName(workflowId + "-execution-" + 0)
                            .withStatus(ExecutionStatus.RUNNING)
                            .build();
    wingsPersistence.save(workflowExecution);

    logElements = new ArrayList<>();

    elkBeatElement = new LogElement();
    elkBeatElement.setQuery(query);
    elkBeatElement.setClusterLabel("-3");
    elkBeatElement.setHost(host);
    elkBeatElement.setCount(0);
    elkBeatElement.setLogMessage("");
    elkBeatElement.setTimeStamp(0);
    elkBeatElement.setLogCollectionMinute(logCollectionMinute);
    logElements.add(elkBeatElement);

    analysisService.saveLogData(StateType.ELK, accountId, appId, stateExecutionId, workflowId, workflowExecutionId,
        serviceId, ClusterLevel.L1, delegateTaskId, logElements);

    AnalysisContext analysisContext =
        AnalysisContext.builder()
            .accountId(accountId)
            .appId(appId)
            .workflowId(workflowId)
            .workflowExecutionId(workflowExecutionId)
            .stateExecutionId(stateExecutionId)
            .serviceId(serviceId)
            .controlNodes(com.google.common.collect.Sets.newHashSet(host))
            .testNodes(com.google.common.collect.Sets.newHashSet(host))
            .queries(com.google.common.collect.Sets.newHashSet(query.split(",")))
            .isSSL(true)
            .appPort(9090)
            .comparisonStrategy(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS)
            .timeDuration(1)
            .stateType(StateType.ELK)
            .stateBaseUrl(LogAnalysisResource.ELK_RESOURCE_BASE_URL)
            .authToken(AbstractAnalysisState.generateAuthToken("nhUmut2NMcUnsR01OgOz0e51MZ51AqUwrOATJ3fJ"))
            .correlationId(UUID.randomUUID().toString())
            .build();
    JobExecutionContext jobExecutionContext = mock(JobExecutionContext.class);
    JobDataMap jobDataMap = mock(JobDataMap.class);
    when(jobDataMap.getLong("timestamp")).thenReturn(System.currentTimeMillis());
    when(jobDataMap.getString("jobParams")).thenReturn(JsonUtils.asJson(analysisContext));
    when(jobDataMap.getString("delegateTaskId")).thenReturn(UUID.randomUUID().toString());
    when(jobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);
    when(jobExecutionContext.getScheduler()).thenReturn(mock(Scheduler.class));
    when(jobExecutionContext.getJobDetail()).thenReturn(mock(JobDetail.class));

    new LogAnalysisTask(
        analysisService, waitNotifyEngine, delegateService, analysisContext, jobExecutionContext, delegateTaskId)
        .run();
    LogMLAnalysisSummary logMLAnalysisSummary =
        analysisService.getAnalysisSummary(stateExecutionId, appId, StateType.ELK);
    assertEquals(logMLAnalysisSummary.getAnalysisSummaryMessage(), "No data found for the given queries.");
    LogMLAnalysisRecord logAnalysisRecord =
        analysisService.getLogAnalysisRecords(appId, stateExecutionId, query, StateType.ELK, logCollectionMinute);
    assertNotNull(logAnalysisRecord);
  }
}
