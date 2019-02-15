package io.harness.integration;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder.aWorkflowExecution;
import static software.wings.common.VerificationConstants.DEFAULT_GROUP_NAME;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeBasedTable;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;

import io.harness.VerificationBaseIntegrationTest;
import io.harness.beans.ExecutionStatus;
import io.harness.jobs.LogAnalysisManagerJob.LogAnalysisTask;
import io.harness.jobs.LogMLClusterGenerator;
import io.harness.managerclient.VerificationManagerClient;
import io.harness.managerclient.VerificationManagerClientHelper;
import io.harness.rest.RestResponse;
import io.harness.serializer.JsonUtils;
import io.harness.service.intfc.LearningEngineService;
import io.harness.service.intfc.LogAnalysisService;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import software.wings.api.PhaseElement.PhaseElementBuilder;
import software.wings.api.ServiceElement;
import software.wings.beans.CountsByStatuses;
import software.wings.beans.FeatureFlag;
import software.wings.beans.FeatureName;
import software.wings.beans.SettingAttribute;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.AnalysisServiceImpl;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData;
import software.wings.service.impl.analysis.LogAnalysisExecutionData;
import software.wings.service.impl.analysis.LogClusterContext;
import software.wings.service.impl.analysis.LogDataRecord;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.analysis.LogMLAnalysisRecord;
import software.wings.service.impl.analysis.LogMLAnalysisSummary;
import software.wings.service.impl.analysis.LogMLFeedback;
import software.wings.service.impl.analysis.LogMLFeedbackRecord;
import software.wings.service.impl.analysis.LogRequest;
import software.wings.service.impl.splunk.SplunkAnalysisCluster;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateMachine;
import software.wings.sm.StateType;
import software.wings.sm.states.ApprovalState;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

/**
 * Created by rsingh on 8/17/17.
 */
@SuppressWarnings("ALL")
public class LogMLIntegrationTest extends VerificationBaseIntegrationTest {
  private static final StateType[] logAnalysisStates = new StateType[] {StateType.SPLUNKV2, StateType.ELK};
  private Set<String> hosts = new HashSet<>();
  @Inject private LogAnalysisService analysisService;
  @Inject private VerificationManagerClientHelper managerClientHelper;
  @Inject private VerificationManagerClient managerClient;
  @Inject private LearningEngineService learningEngineService;
  private Random r;
  private String appId;
  private String stateExecutionId;
  private String workflowId;
  private String workflowExecutionId;
  private String serviceId;
  private String delegateTaskId;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    loginAdminUser();
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
    r = new Random(System.currentTimeMillis());
  }

  private SplunkAnalysisCluster getRandomClusterEvent() {
    SplunkAnalysisCluster analysisCluster = new SplunkAnalysisCluster();
    analysisCluster.setCluster_label(r.nextInt(100));
    analysisCluster.setAnomalous_counts(Lists.newArrayList(r.nextInt(100), r.nextInt(100), r.nextInt(100)));
    analysisCluster.setText(UUID.randomUUID().toString());
    analysisCluster.setTags(
        Lists.newArrayList(UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString()));
    analysisCluster.setDiff_tags(
        Lists.newArrayList(UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString()));
    analysisCluster.setX(r.nextDouble());
    analysisCluster.setY(r.nextDouble());
    analysisCluster.setUnexpected_freq(r.nextBoolean());
    List<Map> frequencyMapList = new ArrayList<>();
    for (int i = 0; i < 1 + r.nextInt(10); i++) {
      Map<String, Integer> frequencyMap = new HashMap<>();
      frequencyMap.put("count", r.nextInt(100));
      frequencyMapList.add(frequencyMap);
    }

    analysisCluster.setMessage_frequencies(frequencyMapList);
    return analysisCluster;
  }

  @Test
  public void saveAnalysisSummaryControlClusters() throws Exception {
    loginAdminUser();
    int numOfControlClusters = 1 + r.nextInt(10);
    int numOfClusters = 4;
    List<Map<String, Map<String, SplunkAnalysisCluster>>> clusters = new ArrayList<>();
    Set<String> hosts = new HashSet<>();
    Map<String, List<SplunkAnalysisCluster>> controlEvents = new HashMap<>();
    controlEvents.put("xyz", Lists.newArrayList(getRandomClusterEvent()));
    for (int i = 0; i < numOfControlClusters; i++) {
      for (int j = 0; j < numOfClusters; j++) {
        List<SplunkAnalysisCluster> clusterEvents = new ArrayList<>();
        Map<String, Map<String, SplunkAnalysisCluster>> controlClusters = new HashMap<>();
        SplunkAnalysisCluster cluster = getRandomClusterEvent();
        clusterEvents.add(cluster);
        Map<String, SplunkAnalysisCluster> hostMap = new HashMap<>();
        String host = UUID.randomUUID().toString() + ".harness.com";
        hostMap.put(host, cluster);
        hosts.add(host);
        controlClusters.put(UUID.randomUUID().toString(), hostMap);
        clusters.add(controlClusters);
      }
    }

    LogMLAnalysisRecord record = new LogMLAnalysisRecord();
    record.setStateExecutionId(stateExecutionId);
    record.setAppId(appId);
    record.setStateType(StateType.SPLUNKV2);
    record.setLogCollectionMinute(0);
    record.setQuery(UUID.randomUUID().toString());
    record.setControl_events(controlEvents);
    record.setControl_clusters(clusters.get(0));
    record.setTest_clusters(clusters.get(1));
    record.setUnknown_clusters(clusters.get(2));
    record.setIgnore_clusters(clusters.get(3));

    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setUuid(stateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setDisplayName("log");

    Map<String, StateExecutionData> stateExecutionMap = new HashMap<>();
    stateExecutionMap.put("log", LogAnalysisExecutionData.builder().build());
    stateExecutionInstance.setStateExecutionMap(stateExecutionMap);

    wingsPersistence.saveIgnoringDuplicateKeys(Collections.singletonList(stateExecutionInstance));

    WebTarget target = client.target(VERIFICATION_API_BASE + "/" + LogAnalysisResource.LOG_ANALYSIS
        + LogAnalysisResource.ANALYSIS_STATE_SAVE_ANALYSIS_RECORDS_URL + "?accountId=" + accountId
        + "&applicationId=" + appId + "&stateExecutionId=" + stateExecutionId + "&logCollectionMinute=" + 0
        + "&isBaselineCreated=" + true + "&taskId=" + generateUuid() + "&stateType=" + StateType.SPLUNKV2);
    Response restResponse = getRequestBuilderWithLearningAuthHeader(target).post(entity(record, APPLICATION_JSON));
    assertEquals(restResponse.getStatus(), HttpStatus.SC_OK);

    target = client.target(API_BASE + "/" + LogAnalysisResource.LOG_ANALYSIS
        + LogAnalysisResource.ANALYSIS_STATE_GET_ANALYSIS_SUMMARY_URL + "?accountId=" + accountId
        + "&applicationId=" + appId + "&stateExecutionId=" + stateExecutionId + "&stateType=" + StateType.SPLUNKV2);

    RestResponse<LogMLAnalysisSummary> analysisResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<LogMLAnalysisSummary>>() {});

    assertNotNull(analysisResponse.getResource());
    assertNotNull(analysisResponse.getResource().getControlClusters());
    assertNotNull(analysisResponse.getResource().getTestClusters());
    assertNotNull(analysisResponse.getResource().getIgnoreClusters());
    assertNotNull(analysisResponse.getResource().getIgnoreClusters());
  }

  @Test
  public void testFeatureflagDemoSuccess() {
    loginAdminUser();
    wingsPersistence.delete(
        wingsPersistence.createQuery(FeatureFlag.class, excludeAuthority).filter("name", FeatureName.CV_DEMO.name()));

    wingsPersistence.save(FeatureFlag.builder().name(FeatureName.CV_DEMO.name()).enabled(false).build());

    wingsPersistence.update(wingsPersistence.createQuery(FeatureFlag.class, excludeAuthority).filter("name", "CV_DEMO"),
        wingsPersistence.createUpdateOperations(FeatureFlag.class).addToSet("accountIds", "xyz"));

    wingsPersistence.delete(
        wingsPersistence.createQuery(SettingAttribute.class).filter("accountId", accountId).filter("name", "elk_prod"));
    String serverConfigId = wingsPersistence.save(
        SettingAttribute.Builder.aSettingAttribute().withAccountId(accountId).withName("elk_prod").build());

    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setUuid(stateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.SUCCESS);
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setDisplayName("Relic_Fail");
    Map<String, StateExecutionData> hashMap = new HashMap();
    hashMap.put("Relic_Fail", LogAnalysisExecutionData.builder().serverConfigId(serverConfigId).build());
    stateExecutionInstance.setStateExecutionMap(hashMap);
    wingsPersistence.saveIgnoringDuplicateKeys(Collections.singletonList(stateExecutionInstance));

    int numOfTestClusters = 1 + r.nextInt(10);
    List<SplunkAnalysisCluster> clusterEvents = new ArrayList<>();
    Map<String, Map<String, SplunkAnalysisCluster>> testClusters = new HashMap<>();
    Set<String> hosts = new HashSet<>();
    Map<String, List<SplunkAnalysisCluster>> controlEvents = new HashMap<>();
    controlEvents.put("xyz", Lists.newArrayList(getRandomClusterEvent()));
    for (int i = 0; i < numOfTestClusters; i++) {
      SplunkAnalysisCluster cluster = getRandomClusterEvent();
      clusterEvents.add(cluster);
      Map<String, SplunkAnalysisCluster> hostMap = new HashMap<>();
      String host = UUID.randomUUID().toString() + ".harness.com";
      hostMap.put(host, cluster);
      hosts.add(host);
      testClusters.put(UUID.randomUUID().toString(), hostMap);
    }
    LogMLAnalysisRecord record = new LogMLAnalysisRecord();
    record.setStateExecutionId("CV-Demo-LOG-Success-ELK");
    record.setAppId("CV-Demo");
    record.setStateType(StateType.ELK);
    record.setLogCollectionMinute(0);
    record.setQuery("cv-demo-query");
    record.setControl_events(controlEvents);
    record.setTest_clusters(testClusters);
    analysisService.saveLogAnalysisRecords(record, StateType.ELK, Optional.empty());

    WebTarget getTarget = client.target(API_BASE + "/" + LogAnalysisResource.LOG_ANALYSIS
        + LogAnalysisResource.ANALYSIS_STATE_GET_ANALYSIS_SUMMARY_URL + "?accountId=" + accountId
        + "&applicationId=" + appId + "&stateExecutionId=" + stateExecutionId + "&stateType=" + StateType.ELK);

    RestResponse<LogMLAnalysisSummary> restResponse =
        getRequestBuilderWithAuthHeader(getTarget).get(new GenericType<RestResponse<LogMLAnalysisSummary>>() {});
    assertNull(restResponse.getResource());

    wingsPersistence.update(wingsPersistence.createQuery(FeatureFlag.class, excludeAuthority).filter("name", "CV_DEMO"),
        wingsPersistence.createUpdateOperations(FeatureFlag.class).addToSet("accountIds", accountId));

    restResponse =
        getRequestBuilderWithAuthHeader(getTarget).get(new GenericType<RestResponse<LogMLAnalysisSummary>>() {});
    assertNotNull(restResponse.getResource());

    LogMLAnalysisSummary summary = restResponse.getResource();
    assertEquals("cv-demo-query", summary.getQuery());
  }

  @Test
  public void testFeatureflagDemoFail() {
    loginAdminUser();
    wingsPersistence.delete(
        wingsPersistence.createQuery(FeatureFlag.class, excludeAuthority).filter("name", FeatureName.CV_DEMO.name()));

    wingsPersistence.save(FeatureFlag.builder().name(FeatureName.CV_DEMO.name()).enabled(false).build());

    wingsPersistence.update(wingsPersistence.createQuery(FeatureFlag.class, excludeAuthority).filter("name", "CV_DEMO"),
        wingsPersistence.createUpdateOperations(FeatureFlag.class).addToSet("accountIds", "xyz"));

    wingsPersistence.delete(wingsPersistence.createQuery(SettingAttribute.class).filter("name", "elk_dev"));

    String serverConfigId = wingsPersistence.save(
        SettingAttribute.Builder.aSettingAttribute().withAccountId(accountId).withName("elk_dev").build());
    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setUuid(stateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.FAILED);
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setDisplayName("Relic_Fail");
    Map<String, StateExecutionData> hashMap = new HashMap();
    hashMap.put("Relic_Fail", LogAnalysisExecutionData.builder().serverConfigId(serverConfigId).build());
    stateExecutionInstance.setStateExecutionMap(hashMap);
    wingsPersistence.saveIgnoringDuplicateKeys(Collections.singletonList(stateExecutionInstance));

    int numOfTestClusters = 1 + r.nextInt(10);
    List<SplunkAnalysisCluster> clusterEvents = new ArrayList<>();
    Map<String, Map<String, SplunkAnalysisCluster>> testClusters = new HashMap<>();
    Set<String> hosts = new HashSet<>();
    Map<String, List<SplunkAnalysisCluster>> controlEvents = new HashMap<>();
    controlEvents.put("xyz", Lists.newArrayList(getRandomClusterEvent()));
    for (int i = 0; i < numOfTestClusters; i++) {
      SplunkAnalysisCluster cluster = getRandomClusterEvent();
      clusterEvents.add(cluster);
      Map<String, SplunkAnalysisCluster> hostMap = new HashMap<>();
      String host = UUID.randomUUID().toString() + ".harness.com";
      hostMap.put(host, cluster);
      hosts.add(host);
      testClusters.put(UUID.randomUUID().toString(), hostMap);
    }
    LogMLAnalysisRecord record = new LogMLAnalysisRecord();
    record.setStateExecutionId("CV-Demo-LOG-Failure-ELK");
    record.setAppId("CV-Demo");
    record.setStateType(StateType.ELK);
    record.setLogCollectionMinute(0);
    record.setQuery("cv-demo-query");
    record.setControl_events(controlEvents);
    record.setTest_clusters(testClusters);
    analysisService.saveLogAnalysisRecords(record, StateType.ELK, Optional.empty());

    WebTarget getTarget = client.target(API_BASE + "/" + LogAnalysisResource.LOG_ANALYSIS
        + LogAnalysisResource.ANALYSIS_STATE_GET_ANALYSIS_SUMMARY_URL + "?accountId=" + accountId
        + "&applicationId=" + appId + "&stateExecutionId=" + stateExecutionId + "&stateType=" + StateType.ELK);

    RestResponse<LogMLAnalysisSummary> restResponse =
        getRequestBuilderWithAuthHeader(getTarget).get(new GenericType<RestResponse<LogMLAnalysisSummary>>() {});
    assertNull(restResponse.getResource());

    wingsPersistence.update(wingsPersistence.createQuery(FeatureFlag.class, excludeAuthority).filter("name", "CV_DEMO"),
        wingsPersistence.createUpdateOperations(FeatureFlag.class).addToSet("accountIds", accountId));

    restResponse =
        getRequestBuilderWithAuthHeader(getTarget).get(new GenericType<RestResponse<LogMLAnalysisSummary>>() {});
    assertNotNull(restResponse.getResource());

    LogMLAnalysisSummary summary = restResponse.getResource();
    assertEquals("cv-demo-query", summary.getQuery());
  }

  @Test
  public void testFirstLevelClustering() throws Exception {
    for (String host : hosts) {
      File file = new File(getClass().getClassLoader().getResource("./elk/" + host + ".json").getFile());

      List<LogDataRecord> logDataRecords =
          readLogDataRecordsFromFile(file, appId, workflowId, workflowExecutionId, stateExecutionId);
      Map<Long, List<LogElement>> recordsByMinute = splitRecordsByMinute(logDataRecords);

      final String serviceId = logDataRecords.get(0).getServiceId();
      final String query = ".*exception.*";

      StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
      stateExecutionInstance.setUuid(stateExecutionId);
      stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
      stateExecutionInstance.setAppId(appId);
      wingsPersistence.saveIgnoringDuplicateKeys(Collections.singletonList(stateExecutionInstance));

      wingsPersistence.saveIgnoringDuplicateKeys(logDataRecords);

      for (long logCollectionMinute : recordsByMinute.keySet()) {
        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        final LogClusterContext logClusterContext = LogClusterContext.builder()
                                                        .accountId(accountId)
                                                        .appId(appId)
                                                        .workflowExecutionId(workflowExecutionId)
                                                        .workflowId(workflowId)
                                                        .stateExecutionId(stateExecutionId)
                                                        .serviceId(serviceId)
                                                        .controlNodes(Sets.newHashSet(Collections.singletonList(host)))
                                                        .testNodes(Sets.newHashSet(Collections.singletonList(host)))
                                                        .query(query)
                                                        .isSSL(true)
                                                        .appPort(9090)
                                                        .stateType(StateType.ELK)
                                                        .stateBaseUrl(LogAnalysisResource.ELK_RESOURCE_BASE_URL)
                                                        .build();

        new LogMLClusterGenerator(learningEngineService, logClusterContext, ClusterLevel.L0, ClusterLevel.L1,
            new LogRequest(query, appId, stateExecutionId, workflowId, serviceId,
                Sets.newHashSet(Collections.singletonList(host)), logCollectionMinute))
            .run();

        final LogRequest logRequest = new LogRequest(
            query, appId, stateExecutionId, workflowId, serviceId, Collections.singleton(host), logCollectionMinute);

        WebTarget getTarget = client.target(VERIFICATION_API_BASE + "/" + LogAnalysisResource.LOG_ANALYSIS
            + LogAnalysisResource.ANALYSIS_STATE_GET_LOG_URL + "?accountId=" + accountId
            + "&clusterLevel=" + ClusterLevel.L1.name()
            + "&compareCurrent=true&workflowExecutionId=" + workflowExecutionId + "&stateType=" + StateType.ELK);
        boolean succeess = false;
        for (int i = 0; i < 10; i++) {
          RestResponse<List<LogDataRecord>> restResponse = getRequestBuilderWithLearningAuthHeader(getTarget).post(
              entity(logRequest, APPLICATION_JSON), new GenericType<RestResponse<List<LogDataRecord>>>() {});
          if (restResponse.getResource().size() == 15) {
            succeess = true;
            break;
          } else {
            Thread.sleep(TimeUnit.SECONDS.toMillis(1));
          }
        }

        assertTrue("failed for " + host + " for minute " + logCollectionMinute, succeess);
      }
    }
  }

  private Map<Long, List<LogElement>> splitRecordsByMinute(List<LogDataRecord> logDataRecords) {
    final Map<Long, List<LogElement>> rv = new HashMap<>();
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

  private List<LogDataRecord> readLogDataRecordsFromFile(File file, String appId, String workflowId,
      String workflowExecutionId, String stateExecutionId) throws IOException {
    final Gson gson = new Gson();
    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      Type type = new TypeToken<List<LogDataRecord>>() {}.getType();
      List<LogDataRecord> rv = gson.fromJson(br, type);
      rv.forEach(logDataRecord -> {
        logDataRecord.setAppId(appId);
        logDataRecord.setAppId(appId);
        logDataRecord.setWorkflowId(workflowId);
        logDataRecord.setWorkflowExecutionId(workflowExecutionId);
        logDataRecord.setStateExecutionId(stateExecutionId);
      });
      return rv;
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
  public void controlButNoTestData() throws IOException, InterruptedException {
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
            .withServiceIds(Lists.newArrayList(serviceId))
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

    analysisService.saveLogData(StateType.SPLUNKV2, accountId, appId, null, prevStateExecutionId, workflowId,
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

    analysisService.saveLogData(StateType.SPLUNKV2, accountId, appId, null, stateExecutionId, workflowId,
        workflowExecutionId, serviceId, ClusterLevel.L1, delegateTaskId, logElements);

    ContinuousVerificationExecutionMetaData metaData = ContinuousVerificationExecutionMetaData.builder()
                                                           .stateType(StateType.SPLUNKV2)
                                                           .workflowId(workflowId)
                                                           .workflowExecutionId(workFlowExecutionId)
                                                           .executionStatus(ExecutionStatus.SUCCESS)
                                                           .applicationId(appId)
                                                           .workflowStartTs(System.currentTimeMillis())
                                                           .build();
    wingsPersistence.save(metaData);
    final String lastWorkflowExecutionId = analysisService.getLastSuccessfulWorkflowExecutionIdWithLogs(
        StateType.SPLUNKV2, appId, serviceId, workflowId, query);

    AnalysisContext analysisContext = AnalysisContext.builder()
                                          .accountId(accountId)
                                          .appId(appId)
                                          .workflowId(workflowId)
                                          .workflowExecutionId(workflowExecutionId)
                                          .prevWorkflowExecutionId(lastWorkflowExecutionId)
                                          .stateExecutionId(stateExecutionId)
                                          .serviceId(serviceId)
                                          .controlNodes(Collections.singletonMap(host, DEFAULT_GROUP_NAME))
                                          .testNodes(Collections.singletonMap(host, DEFAULT_GROUP_NAME))
                                          .query(query)
                                          .isSSL(true)
                                          .appPort(9090)
                                          .comparisonStrategy(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS)
                                          .timeDuration(1)
                                          .stateType(StateType.SPLUNKV2)
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

    new LogAnalysisTestJob(analysisService, analysisContext, jobExecutionContext, delegateTaskId, learningEngineService,
        managerClient, managerClientHelper, wingsPersistence)
        .call();
    Thread.sleep(TimeUnit.SECONDS.toMillis(20));
    LogMLAnalysisSummary logMLAnalysisSummary =
        analysisService.getAnalysisSummary(stateExecutionId, appId, StateType.SPLUNKV2);
    assertEquals(1, logMLAnalysisSummary.getControlClusters().size());
    assertEquals(0, logMLAnalysisSummary.getTestClusters().size());
    assertEquals("No new data for the given queries. Showing baseline data if any.",
        logMLAnalysisSummary.getAnalysisSummaryMessage());
  }

  private String setupNoControlTest(String query, String host) {
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

    analysisService.saveLogData(StateType.SUMO, accountId, appId, null, prevStateExecutionId, workflowId,
        workFlowExecutionId, serviceId, ClusterLevel.L2, delegateTaskId, logElements);

    stateExecutionInstance.setStatus(ExecutionStatus.SUCCESS);
    wingsPersistence.save(stateExecutionInstance);

    stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setUuid(stateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    stateExecutionInstance.getContextElements().push(
        PhaseElementBuilder.aPhaseElement()
            .withServiceElement(ServiceElement.Builder.aServiceElement().withUuid(serviceId).build())
            .build());
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

    LogElement logElement = new LogElement(query, "0", host, 0, 1, "Hello World", logCollectionMinute);
    logElements.add(logElement);

    logElements.add(splunkHeartBeatElement);

    analysisService.saveLogData(StateType.SUMO, accountId, appId, null, stateExecutionId, workflowId,
        workflowExecutionId, serviceId, ClusterLevel.L1, delegateTaskId, logElements);
    return workFlowExecutionId;
  }

  @Test
  public void testButNoControlDataFirstExecution() throws IOException, InterruptedException {
    final String query = UUID.randomUUID().toString();
    final String host = UUID.randomUUID().toString();
    setupNoControlTest(query, host);

    AnalysisContext analysisContext = AnalysisContext.builder()
                                          .accountId(accountId)
                                          .appId(appId)
                                          .workflowId(workflowId)
                                          .workflowExecutionId(workflowExecutionId)
                                          .stateExecutionId(stateExecutionId)
                                          .serviceId(serviceId)
                                          .prevWorkflowExecutionId(null)
                                          .controlNodes(Collections.singletonMap(host, DEFAULT_GROUP_NAME))
                                          .testNodes(Collections.singletonMap(host, DEFAULT_GROUP_NAME))
                                          .query(query)
                                          .isSSL(true)
                                          .appPort(9090)
                                          .comparisonStrategy(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS)
                                          .timeDuration(1)
                                          .stateType(StateType.SUMO)
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

    new LogAnalysisTestJob(analysisService, analysisContext, jobExecutionContext, delegateTaskId, learningEngineService,
        managerClient, managerClientHelper, wingsPersistence)
        .call();
    LogMLAnalysisSummary logMLAnalysisSummary =
        analysisService.getAnalysisSummary(stateExecutionId, appId, StateType.SUMO);
    assertEquals("No baseline data for the given query was found.", logMLAnalysisSummary.getAnalysisSummaryMessage());
    LogMLAnalysisRecord logAnalysisRecord =
        analysisService.getLogAnalysisRecords(appId, stateExecutionId, query, StateType.SUMO, 0);
    assertFalse(logAnalysisRecord.isBaseLineCreated());
    assertEquals(1, logAnalysisRecord.getControl_clusters().size());
    assertTrue(isEmpty(logAnalysisRecord.getTest_clusters()));
    assertTrue(isEmpty(logAnalysisRecord.getTest_events()));

    LogMLFeedback mlFeedback = LogMLFeedback.builder()
                                   .stateExecutionId(stateExecutionId)
                                   .logMLFeedbackType(AnalysisServiceImpl.LogMLFeedbackType.IGNORE_ALWAYS)
                                   .comment("awesome")
                                   .clusterType(AnalysisServiceImpl.CLUSTER_TYPE.TEST)
                                   .clusterLabel(0)
                                   .appId(appId)
                                   .build();
    WebTarget getTarget = client.target(API_BASE + "/" + LogAnalysisResource.LOG_ANALYSIS
        + LogAnalysisResource.ANALYSIS_USER_FEEDBACK + "?accountId=" + accountId + "&stateType=" + StateType.SUMO);
    RestResponse<Boolean> restResponse = getRequestBuilderWithAuthHeader(getTarget).post(
        entity(mlFeedback, APPLICATION_JSON), new GenericType<RestResponse<Boolean>>() {});

    LogMLAnalysisSummary analysisSummary = analysisService.getAnalysisSummary(stateExecutionId, appId, StateType.SUMO);

    assertEquals(AnalysisServiceImpl.LogMLFeedbackType.IGNORE_ALWAYS,
        analysisSummary.getTestClusters().get(0).getLogMLFeedbackType());

    assertTrue(restResponse.getResource());
  }

  @Test
  public void testNoControlNotFirstExecution() throws Exception {
    final String query = UUID.randomUUID().toString();
    final String host = UUID.randomUUID().toString();
    String workFlowExecutionId = setupNoControlTest(query, host);

    ContinuousVerificationExecutionMetaData metaData = ContinuousVerificationExecutionMetaData.builder()
                                                           .stateType(StateType.SUMO)
                                                           .workflowId(workflowId)
                                                           .workflowExecutionId(workFlowExecutionId)
                                                           .executionStatus(ExecutionStatus.SUCCESS)
                                                           .applicationId(appId)
                                                           .workflowStartTs(System.currentTimeMillis())
                                                           .build();
    wingsPersistence.save(metaData);

    AnalysisContext analysisContext =
        AnalysisContext.builder()
            .accountId(accountId)
            .appId(appId)
            .workflowId(workflowId)
            .workflowExecutionId(workflowExecutionId)
            .stateExecutionId(stateExecutionId)
            .serviceId(serviceId)
            .prevWorkflowExecutionId(analysisService.getLastSuccessfulWorkflowExecutionIdWithLogs(
                StateType.SUMO, appId, serviceId, workflowId, query))
            .controlNodes(Collections.singletonMap(host, DEFAULT_GROUP_NAME))
            .testNodes(Collections.singletonMap(host, DEFAULT_GROUP_NAME))
            .query(query)
            .isSSL(true)
            .appPort(9090)
            .comparisonStrategy(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS)
            .timeDuration(1)
            .stateType(StateType.SUMO)
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

    new LogAnalysisTestJob(analysisService, analysisContext, jobExecutionContext, delegateTaskId, learningEngineService,
        managerClient, managerClientHelper, wingsPersistence)
        .call();

    LogMLAnalysisSummary logMLAnalysisSummary =
        analysisService.getAnalysisSummary(stateExecutionId, appId, StateType.SUMO);
    //    assertEquals("No baseline data for the given query was found.",
    //    logMLAnalysisSummary.getAnalysisSummaryMessage());
    LogMLAnalysisRecord logAnalysisRecord =
        analysisService.getLogAnalysisRecords(appId, stateExecutionId, query, StateType.SUMO, 0);
    assertTrue(logAnalysisRecord.isBaseLineCreated());
    assertTrue(isEmpty(logAnalysisRecord.getControl_clusters()));
    assertTrue(isEmpty(logAnalysisRecord.getTest_clusters()));
    assertEquals(1, logAnalysisRecord.getUnknown_clusters().size());
    assertFalse(isEmpty(logAnalysisRecord.getTest_events()));
  }

  @Test
  public void noControlandTestData() throws IOException {
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

    analysisService.saveLogData(StateType.ELK, accountId, appId, null, prevStateExecutionId, workflowId,
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

    elkBeatElement = new LogElement();
    elkBeatElement.setQuery(query);
    elkBeatElement.setClusterLabel("-3");
    elkBeatElement.setHost(host);
    elkBeatElement.setCount(0);
    elkBeatElement.setLogMessage("");
    elkBeatElement.setTimeStamp(0);
    elkBeatElement.setLogCollectionMinute(logCollectionMinute);
    logElements.add(elkBeatElement);

    analysisService.saveLogData(StateType.ELK, accountId, appId, null, stateExecutionId, workflowId,
        workflowExecutionId, serviceId, ClusterLevel.L1, delegateTaskId, logElements);

    AnalysisContext analysisContext = AnalysisContext.builder()
                                          .accountId(accountId)
                                          .appId(appId)
                                          .workflowId(workflowId)
                                          .workflowExecutionId(workflowExecutionId)
                                          .stateExecutionId(stateExecutionId)
                                          .serviceId(serviceId)
                                          .controlNodes(Collections.singletonMap(host, DEFAULT_GROUP_NAME))
                                          .testNodes(Collections.singletonMap(host, DEFAULT_GROUP_NAME))
                                          .query(query)
                                          .isSSL(true)
                                          .appPort(9090)
                                          .comparisonStrategy(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS)
                                          .timeDuration(1)
                                          .stateType(StateType.ELK)
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

    new LogAnalysisTestJob(analysisService, analysisContext, jobExecutionContext, delegateTaskId, learningEngineService,
        managerClient, managerClientHelper, wingsPersistence)
        .call();
    LogMLAnalysisSummary logMLAnalysisSummary =
        analysisService.getAnalysisSummary(stateExecutionId, appId, StateType.ELK);
    assertEquals("No data found for the given queries.", logMLAnalysisSummary.getAnalysisSummaryMessage());
    LogMLAnalysisRecord logAnalysisRecord =
        analysisService.getLogAnalysisRecords(appId, stateExecutionId, query, StateType.ELK, logCollectionMinute);
    assertNotNull(logAnalysisRecord);
  }

  @Test
  public void withControlAndTest() throws IOException, InterruptedException {
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
    String prevWorkFlowExecutionId = wingsPersistence.save(workflowExecution);

    List<LogElement> logElements = new ArrayList<>();

    final String query = UUID.randomUUID().toString();
    Map<String, String> hosts = new HashMap<>();

    for (int i = 0; i < 5; i++) {
      hosts.put("host-" + i, DEFAULT_GROUP_NAME);
    }

    for (int i = 0; i < 5; i++) {
      for (int j = 0; j < 5; j++) {
        LogElement splunkHeartBeatElement = new LogElement();
        splunkHeartBeatElement.setQuery(query);
        splunkHeartBeatElement.setClusterLabel("-3");
        splunkHeartBeatElement.setHost("host-" + j);
        splunkHeartBeatElement.setCount(0);
        splunkHeartBeatElement.setLogMessage("");
        splunkHeartBeatElement.setTimeStamp(0);
        splunkHeartBeatElement.setLogCollectionMinute(i);

        logElements.add(splunkHeartBeatElement);
        if (i % 2 == 0) {
          LogElement logElement = new LogElement(query, "0", "host-" + j, 0, 1, "Hello World " + i, i);
          logElements.add(logElement);
        }
      }
    }

    analysisService.saveLogData(StateType.SUMO, accountId, appId, null, prevStateExecutionId, workflowId,
        prevWorkFlowExecutionId, serviceId, ClusterLevel.L2, delegateTaskId, logElements);

    stateExecutionInstance.setStatus(ExecutionStatus.SUCCESS);
    wingsPersistence.save(stateExecutionInstance);

    stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setUuid(stateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    stateExecutionInstance.getContextElements().push(
        PhaseElementBuilder.aPhaseElement()
            .withServiceElement(ServiceElement.Builder.aServiceElement().withUuid(serviceId).build())
            .build());
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
    for (int i = 0; i < 5; i++) {
      for (int j = 0; j < 5; ++j) {
        LogElement splunkHeartBeatElement = new LogElement();
        splunkHeartBeatElement.setQuery(query);
        splunkHeartBeatElement.setClusterLabel("-3");
        splunkHeartBeatElement.setHost("host-" + j);
        splunkHeartBeatElement.setCount(0);
        splunkHeartBeatElement.setLogMessage("");
        splunkHeartBeatElement.setTimeStamp(0);
        splunkHeartBeatElement.setLogCollectionMinute(i);
        logElements.add(splunkHeartBeatElement);
        if (i % 2 == 0) {
          LogElement logElement = new LogElement(query, "0", "host-" + j, 0, 1, "Hello World", i);
          logElements.add(logElement);
        }
      }
    }

    analysisService.saveLogData(StateType.SUMO, accountId, appId, null, stateExecutionId, workflowId,
        workflowExecutionId, serviceId, ClusterLevel.L1, delegateTaskId, logElements);

    AnalysisContext analysisContext = AnalysisContext.builder()
                                          .accountId(accountId)
                                          .appId(appId)
                                          .workflowId(workflowId)
                                          .workflowExecutionId(workflowExecutionId)
                                          .stateExecutionId(stateExecutionId)
                                          .serviceId(serviceId)
                                          .controlNodes(hosts)
                                          .testNodes(hosts)
                                          .query(query)
                                          .isSSL(true)
                                          .appPort(9090)
                                          .comparisonStrategy(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS)
                                          .timeDuration(5)
                                          .stateType(StateType.SUMO)
                                          .correlationId(UUID.randomUUID().toString())
                                          .prevWorkflowExecutionId(prevWorkFlowExecutionId)
                                          .build();
    JobExecutionContext jobExecutionContext = mock(JobExecutionContext.class);
    JobDataMap jobDataMap = mock(JobDataMap.class);
    when(jobDataMap.getLong("timestamp")).thenReturn(System.currentTimeMillis());
    when(jobDataMap.getString("jobParams")).thenReturn(JsonUtils.asJson(analysisContext));
    when(jobDataMap.getString("delegateTaskId")).thenReturn(UUID.randomUUID().toString());
    when(jobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);
    when(jobExecutionContext.getScheduler()).thenReturn(mock(Scheduler.class));
    when(jobExecutionContext.getJobDetail()).thenReturn(mock(JobDetail.class));

    for (int i = 0; i < 5; ++i) {
      new LogAnalysisTestJob(analysisService, analysisContext, jobExecutionContext, delegateTaskId,
          learningEngineService, managerClient, managerClientHelper, wingsPersistence)
          .call();
    }
    LogMLAnalysisSummary logMLAnalysisSummary =
        analysisService.getAnalysisSummary(stateExecutionId, appId, StateType.SUMO);
    assertEquals(1, logMLAnalysisSummary.getControlClusters().size());
    assertEquals(1, logMLAnalysisSummary.getTestClusters().size());
    assertEquals(0, logMLAnalysisSummary.getUnknownClusters().size());
    assertEquals(5, logMLAnalysisSummary.getControlClusters().get(0).getHostSummary().size());
    assertEquals(5, logMLAnalysisSummary.getTestClusters().get(0).getHostSummary().size());
    assertEquals(
        3, logMLAnalysisSummary.getControlClusters().get(0).getHostSummary().values().iterator().next().getCount());
  }

  @Test
  public void testFetchCorrectLastWorkflowLogsMissingData() {
    final String query = UUID.randomUUID().toString();
    String workflow1 = addWorkflowDataForLogs(true, query);
    String workflow2 = addWorkflowDataForLogs(false, query);
    final String lastWorkflowExecutionId = analysisService.getLastSuccessfulWorkflowExecutionIdWithLogs(
        StateType.SPLUNKV2, appId, serviceId, workflowId, query);
    assertEquals("The baseline workflow should be first one", workflow1, lastWorkflowExecutionId);
  }

  @Test
  public void testFetchCorrectLastWorkflowLogs() {
    final String query = UUID.randomUUID().toString();
    String workflow1 = addWorkflowDataForLogs(true, query);
    String workflow2 = addWorkflowDataForLogs(true, query);
    final String lastWorkflowExecutionId = analysisService.getLastSuccessfulWorkflowExecutionIdWithLogs(
        StateType.SPLUNKV2, appId, serviceId, workflowId, query);
    assertEquals("The baseline workflow should be second one", workflow2, lastWorkflowExecutionId);
  }

  @Test
  public void testFetchCorrectLastWorkflowLogsFirstWorkflow() {
    final String query = UUID.randomUUID().toString();
    final String lastWorkflowExecutionId = analysisService.getLastSuccessfulWorkflowExecutionIdWithLogs(
        StateType.SPLUNKV2, appId, serviceId, workflowId, query);
    assertEquals("The baseline workflow should be null", null, lastWorkflowExecutionId);
  }

  @Test
  public void testFetchCorrectLastWorkflowLogsNoData() {
    final String query = UUID.randomUUID().toString();
    String workflow1 = addWorkflowDataForLogs(false, query);
    String workflow2 = addWorkflowDataForLogs(false, query);
    final String lastWorkflowExecutionId = analysisService.getLastSuccessfulWorkflowExecutionIdWithLogs(
        StateType.SPLUNKV2, appId, serviceId, workflowId, query);
    assertEquals("The baseline workflow should be workflow2", workflow2, lastWorkflowExecutionId);
  }

  private String addWorkflowDataForLogs(boolean withLogData, String query) {
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
            .withServiceIds(Lists.newArrayList(serviceId))
            .withBreakdown(CountsByStatuses.Builder.aCountsByStatuses().withSuccess(1).build())
            .build();
    String workFlowExecutionId = wingsPersistence.save(workflowExecution);

    List<LogElement> logElements = new ArrayList<>();

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

    if (withLogData) {
      LogElement logElement = new LogElement(query, "0", host, 0, 0, "Hello World", logCollectionMinute);
      logElements.add(logElement);
    }

    analysisService.saveLogData(StateType.SPLUNKV2, accountId, appId, null, prevStateExecutionId, workflowId,
        workFlowExecutionId, serviceId, ClusterLevel.L2, delegateTaskId, logElements);

    ContinuousVerificationExecutionMetaData metaData = ContinuousVerificationExecutionMetaData.builder()
                                                           .stateType(StateType.SPLUNKV2)
                                                           .workflowId(workflowId)
                                                           .workflowExecutionId(workFlowExecutionId)
                                                           .executionStatus(ExecutionStatus.SUCCESS)
                                                           .applicationId(appId)
                                                           .workflowStartTs(System.currentTimeMillis())
                                                           .build();
    wingsPersistence.save(metaData);
    return workFlowExecutionId;
  }

  @Test
  public void validateQuery() {
    WebTarget getTarget = client.target(API_BASE + "/" + LogAnalysisResource.ELK_RESOURCE_BASE_URL
        + LogAnalysisResource.VALIDATE_QUERY + "?accountId=" + accountId + "&query=(.*exception.*)");

    RestResponse<Boolean> restResponse =
        getRequestBuilderWithAuthHeader(getTarget).get(new GenericType<RestResponse<Boolean>>() {});
    assertTrue(restResponse.getResource());
  }

  @Test
  public void validateQueryFail() {
    WebTarget getTarget = client.target(API_BASE + "/" + LogAnalysisResource.ELK_RESOURCE_BASE_URL
        + LogAnalysisResource.VALIDATE_QUERY + "?accountId=" + accountId + "&query=(.*exception.*))");

    try {
      getRequestBuilderWithAuthHeader(getTarget).get(new GenericType<RestResponse<Boolean>>() {});
      fail();
    } catch (BadRequestException e) {
      // ignore
    }
  }

  @Test
  public void testGetCurrentExecutionLogs() throws Exception {
    final Random r = new Random();
    final int numOfExecutions = 4;
    final int numOfHosts = 3;
    final int numOfMinutes = 3;
    final int numOfRecords = 5;

    final String workflowId = "some-workflow";
    final String query = "some-query";
    final String appId = "some-application";
    final String serviceId = "some-service";
    final TreeBasedTable<Integer, Integer, Set<LogDataRecord>> addedMessages = TreeBasedTable.create();
    final Set<String> hosts = new HashSet<>();

    WorkflowExecution workflowExecution =
        aWorkflowExecution().withStatus(ExecutionStatus.SUCCESS).withWorkflowId(workflowId).withAppId(appId).build();
    wingsPersistence.save(workflowExecution);
    for (int executionNumber = 1; executionNumber <= numOfExecutions; executionNumber++) {
      final String stateExecutionId = "se" + executionNumber;
      for (int hostNumber = 0; hostNumber < numOfHosts; hostNumber++) {
        final String host = "host" + hostNumber;
        hosts.add(host);
        for (int logCollectionMinute = 0; logCollectionMinute < numOfMinutes; logCollectionMinute++) {
          final long timeStamp = System.currentTimeMillis();
          for (int recordNumber = 0; recordNumber < numOfRecords; recordNumber++) {
            final int count = r.nextInt();
            final String logMessage = "lmsg" + recordNumber;
            final String logMD5Hash = "lmsgHash" + recordNumber;
            final String clusterLabel = "cluster" + recordNumber;

            final LogDataRecord logDataRecord = new LogDataRecord();
            logDataRecord.setStateType(StateType.SPLUNKV2);
            logDataRecord.setWorkflowId(workflowId);
            logDataRecord.setWorkflowExecutionId(workflowExecution.getUuid());
            logDataRecord.setStateExecutionId(stateExecutionId);
            logDataRecord.setQuery(query);
            logDataRecord.setAppId(appId);
            logDataRecord.setClusterLabel(clusterLabel);
            logDataRecord.setHost(host);
            logDataRecord.setTimeStamp(timeStamp);
            logDataRecord.setCount(count);
            logDataRecord.setLogMessage(logMessage);
            logDataRecord.setLogMD5Hash(logMD5Hash);
            logDataRecord.setClusterLevel(ClusterLevel.L0);
            logDataRecord.setLogCollectionMinute(logCollectionMinute);
            logDataRecord.setCreatedAt(timeStamp);
            logDataRecord.setServiceId(serviceId);

            wingsPersistence.save(logDataRecord);

            if (addedMessages.get(executionNumber, logCollectionMinute) == null) {
              addedMessages.put(executionNumber, logCollectionMinute, new HashSet<>());
            }

            addedMessages.get(executionNumber, logCollectionMinute).add(logDataRecord);
          }
          Thread.sleep(10);
        }
      }
    }

    for (int collectionMinute = 0; collectionMinute < numOfMinutes; collectionMinute++) {
      WebTarget target = client.target(VERIFICATION_API_BASE + "/" + LogAnalysisResource.LOG_ANALYSIS
          + "/get-logs?accountId=" + accountId + "&clusterLevel=" + ClusterLevel.L0.name()
          + "&compareCurrent=true&workflowExecutionId=" + workflowExecution.getUuid()
          + "&stateType=" + StateType.SPLUNKV2);
      final LogRequest logRequest = new LogRequest(query, appId, "se2", workflowId, serviceId, hosts, collectionMinute);
      RestResponse<Set<LogDataRecord>> restResponse = getRequestBuilderWithLearningAuthHeader(target).post(
          entity(logRequest, APPLICATION_JSON), new GenericType<RestResponse<Set<LogDataRecord>>>() {});
      assertEquals(
          "failed for minute " + collectionMinute, addedMessages.get(2, collectionMinute), restResponse.getResource());
    }
  }

  @Test
  public void testGetLastExecutionLogs() throws Exception {
    final Random r = new Random();
    final int numOfExecutions = 1;
    final int numOfHosts = 1 + r.nextInt(5);
    final int numOfMinutes = 1 + r.nextInt(10);
    final int numOfRecords = 1 + r.nextInt(10);

    final String workflowId = "some-workflow";
    final String query = "some-query";
    final String appId = "some-application";
    final String serviceId = "some-service";
    final TreeBasedTable<Integer, Integer, Set<LogDataRecord>> addedMessages = TreeBasedTable.create();

    WorkflowExecution workflowExecution = aWorkflowExecution()
                                              .withStatus(ExecutionStatus.SUCCESS)
                                              .withWorkflowId(workflowId)
                                              .withAppId(appId)
                                              .withStateMachineId(UUID.randomUUID().toString())
                                              .build();
    wingsPersistence.save(workflowExecution);
    StateMachine stateMachine = new StateMachine();
    stateMachine.setInitialStateName("some-state");
    stateMachine.setStates(Lists.newArrayList(new ApprovalState(stateMachine.getInitialStateName())));
    stateMachine.setUuid(workflowExecution.getStateMachineId());
    stateMachine.setAppId(appId);
    wingsPersistence.save(stateMachine);
    Set<String> hosts = new HashSet<>();

    for (int executionNumber = 1; executionNumber <= numOfExecutions; executionNumber++) {
      final String stateExecutionId = "se" + executionNumber;
      for (int hostNumber = 0; hostNumber < numOfHosts; hostNumber++) {
        final String host = "host" + hostNumber;
        hosts.add(host);
        for (int logCollectionMinute = 0; logCollectionMinute < numOfMinutes; logCollectionMinute++) {
          final long timeStamp = System.currentTimeMillis();
          for (int recordNumber = 0; recordNumber < numOfRecords; recordNumber++) {
            final int count = r.nextInt();
            final String logMessage = UUID.randomUUID().toString();
            final String logMD5Hash = UUID.randomUUID().toString();
            final String clusterLabel = UUID.randomUUID().toString();

            final LogDataRecord logDataRecord = new LogDataRecord();
            logDataRecord.setStateType(StateType.SPLUNKV2);
            logDataRecord.setWorkflowId(workflowId);
            logDataRecord.setStateExecutionId(stateExecutionId);
            logDataRecord.setQuery(query);
            logDataRecord.setAppId(appId);
            logDataRecord.setClusterLabel(clusterLabel);
            logDataRecord.setHost(host);
            logDataRecord.setTimeStamp(timeStamp);
            logDataRecord.setCount(count);
            logDataRecord.setLogMessage(logMessage);
            logDataRecord.setLogMD5Hash(logMD5Hash);
            logDataRecord.setClusterLevel(ClusterLevel.L0);
            logDataRecord.setLogCollectionMinute(logCollectionMinute);
            logDataRecord.setCreatedAt(timeStamp);
            logDataRecord.setServiceId(serviceId);
            logDataRecord.setWorkflowExecutionId(workflowExecution.getUuid());
            wingsPersistence.save(logDataRecord);

            if (addedMessages.get(executionNumber, logCollectionMinute) == null) {
              addedMessages.put(executionNumber, logCollectionMinute, new HashSet<>());
            }

            addedMessages.get(executionNumber, logCollectionMinute).add(logDataRecord);
          }
          Thread.sleep(10);
        }
      }
    }

    for (int collectionMinute = 0; collectionMinute < numOfMinutes; collectionMinute++) {
      WebTarget target = client.target(VERIFICATION_API_BASE + "/" + LogAnalysisResource.LOG_ANALYSIS
          + "/get-logs?accountId=" + accountId + "&clusterLevel=" + ClusterLevel.L0.name()
          + "&compareCurrent=false&workflowExecutionId=" + workflowExecution.getUuid()
          + "&stateType=" + StateType.SPLUNKV2);
      final LogRequest logRequest =
          new LogRequest(query, appId, UUID.randomUUID().toString(), workflowId, serviceId, hosts, collectionMinute);
      RestResponse<Set<LogDataRecord>> restResponse = getRequestBuilderWithLearningAuthHeader(target).post(
          entity(logRequest, APPLICATION_JSON), new GenericType<RestResponse<Set<LogDataRecord>>>() {});
      assertEquals("failed for minute " + collectionMinute, addedMessages.get(numOfExecutions, collectionMinute),
          restResponse.getResource());
    }
  }

  private class LogAnalysisTestJob extends LogAnalysisTask {
    WingsPersistence wingsPersistence;
    String stateExecutionId;

    LogAnalysisTestJob(LogAnalysisService analysisService, AnalysisContext context,
        JobExecutionContext jobExecutionContext, String delegateTaskId, LearningEngineService learningEngineService,
        VerificationManagerClient client, VerificationManagerClientHelper managerClient,
        WingsPersistence wingsPersistence) {
      super(
          analysisService, context, jobExecutionContext, delegateTaskId, learningEngineService, client, managerClient);
      this.wingsPersistence = wingsPersistence;
      stateExecutionId = context.getStateExecutionId();
    }

    @Override
    protected void preProcess(long logAnalysisMinute, String query, Set<String> nodes) {
      super.preProcess(logAnalysisMinute, query, nodes);
      try {
        Thread.sleep(TimeUnit.SECONDS.toMillis(10));
      } catch (InterruptedException e) {
        logger.error("", e);
      }
    }

    @Override
    public Long call() {
      Long lastAnalysisMinute = super.call();
      try {
        LogDataRecord record = null;
        for (int i = 0; i < 10; i++) {
          Thread.sleep(TimeUnit.SECONDS.toMillis(5));
          record = wingsPersistence.createQuery(LogDataRecord.class)
                       .filter("stateExecutionId", stateExecutionId)
                       .filter("clusterLevel", "HF")
                       .filter("logCollectionMinute", lastAnalysisMinute)
                       .get();
          if (record != null) {
            return lastAnalysisMinute;
          }
        }
      } catch (InterruptedException e) {
        logger.error("", e);
      }

      throw new IllegalStateException(
          "for stateExecutionId " + stateExecutionId + " logCollectionMinute " + lastAnalysisMinute + " no HF found");
    }
  }

  @Test
  public void onlyFeedback() throws IOException, InterruptedException {
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
            .withServiceIds(Lists.newArrayList(serviceId))
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

    analysisService.saveLogData(StateType.SPLUNKV2, accountId, appId, null, prevStateExecutionId, workflowId,
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

    analysisService.saveLogData(StateType.SPLUNKV2, accountId, appId, null, stateExecutionId, workflowId,
        workflowExecutionId, serviceId, ClusterLevel.L1, delegateTaskId, logElements);

    String logmd5Hash = DigestUtils.md5Hex("hello");

    LogMLFeedbackRecord mlFeedbackRecord = LogMLFeedbackRecord.builder()
                                               .appId(appId)
                                               .serviceId(serviceId)
                                               .workflowId(workflowId)
                                               .workflowExecutionId(UUID.randomUUID().toString())
                                               .stateExecutionId(UUID.randomUUID().toString())
                                               .logMessage("ignore")
                                               .logMLFeedbackType(AnalysisServiceImpl.LogMLFeedbackType.IGNORE_ALWAYS)
                                               .clusterLabel(0)
                                               .clusterType(AnalysisServiceImpl.CLUSTER_TYPE.TEST)
                                               .logMD5Hash(logmd5Hash)
                                               .stateType(StateType.SPLUNKV2)
                                               .comment("None")
                                               .build();

    wingsPersistence.save(mlFeedbackRecord);

    final String lastWorkflowExecutionId = analysisService.getLastSuccessfulWorkflowExecutionIdWithLogs(
        StateType.SPLUNKV2, appId, serviceId, workflowId, query);

    AnalysisContext analysisContext = AnalysisContext.builder()
                                          .accountId(accountId)
                                          .appId(appId)
                                          .workflowId(workflowId)
                                          .workflowExecutionId(workflowExecutionId)
                                          .prevWorkflowExecutionId(lastWorkflowExecutionId)
                                          .stateExecutionId(stateExecutionId)
                                          .serviceId(serviceId)
                                          .controlNodes(Collections.singletonMap(host, DEFAULT_GROUP_NAME))
                                          .testNodes(Collections.singletonMap(host, DEFAULT_GROUP_NAME))
                                          .query(query)
                                          .isSSL(true)
                                          .appPort(9090)
                                          .comparisonStrategy(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS)
                                          .timeDuration(1)
                                          .stateType(StateType.SPLUNKV2)
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

    new LogAnalysisTestJob(analysisService, analysisContext, jobExecutionContext, delegateTaskId, learningEngineService,
        managerClient, managerClientHelper, wingsPersistence)
        .call();
    Thread.sleep(TimeUnit.SECONDS.toMillis(20));
    LogMLAnalysisRecord logAnalysisRecord =
        analysisService.getLogAnalysisRecords(appId, stateExecutionId, query, StateType.SPLUNKV2, 0);
    assertEquals(1, logAnalysisRecord.getControl_events().size());
    assertEquals(0, logAnalysisRecord.getTest_events().size());
    assertEquals("ignore", logAnalysisRecord.getControl_events().get("20000").get(0).getText());
  }
}
