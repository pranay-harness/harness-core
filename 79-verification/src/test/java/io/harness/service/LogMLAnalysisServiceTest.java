package io.harness.service;

import static io.harness.data.encoding.EncodingUtils.compressString;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.service.LearningEngineAnalysisServiceImpl.BACKOFF_LIMIT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.sm.StateType.ELK;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;

import io.harness.VerificationBaseTest;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.managerclient.VerificationManagerClient;
import io.harness.metrics.HarnessMetricRegistry;
import io.harness.persistence.ReadPref;
import io.harness.rest.RestResponse;
import io.harness.rule.RealMongo;
import io.harness.serializer.JsonUtils;
import io.harness.service.intfc.LearningEngineService;
import io.harness.service.intfc.LogAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import retrofit2.Call;
import retrofit2.Response;
import software.wings.api.PhaseElement.PhaseElementBuilder;
import software.wings.api.ServiceElement;
import software.wings.beans.FeatureName;
import software.wings.beans.WorkflowExecution;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.metrics.RiskLevel;
import software.wings.service.impl.MongoDataStoreServiceImpl;
import software.wings.service.impl.WorkflowExecutionServiceImpl;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.AnalysisContext.AnalysisContextKeys;
import software.wings.service.impl.analysis.AnalysisServiceImpl;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData;
import software.wings.service.impl.analysis.ExperimentalLogMLAnalysisRecord;
import software.wings.service.impl.analysis.LogDataRecord;
import software.wings.service.impl.analysis.LogDataRecord.LogDataRecordKeys;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.analysis.LogMLAnalysisRecord;
import software.wings.service.impl.analysis.LogMLAnalysisRecord.LogMLAnalysisRecordKeys;
import software.wings.service.impl.analysis.LogMLAnalysisSummary;
import software.wings.service.impl.analysis.LogMLClusterSummary;
import software.wings.service.impl.analysis.LogMLFeedback;
import software.wings.service.impl.analysis.LogMLFeedbackRecord;
import software.wings.service.impl.analysis.LogRequest;
import software.wings.service.impl.analysis.MLAnalysisType;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask.LearningEngineAnalysisTaskKeys;
import software.wings.service.impl.newrelic.LearningEngineExperimentalAnalysisTask;
import software.wings.service.impl.splunk.SplunkAnalysisCluster;
import software.wings.service.impl.splunk.SplunkAnalysisCluster.MessageFrequency;
import software.wings.service.impl.verification.CV24x7DashboardServiceImpl;
import software.wings.service.impl.verification.CVConfigurationServiceImpl;
import software.wings.service.intfc.DataStoreService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.verification.CV24x7DashboardService;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.sm.states.ElkAnalysisState;
import software.wings.verification.log.LogsCVConfiguration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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

/**
 * Created by rsingh on 9/27/17.
 */
@Slf4j
public class LogMLAnalysisServiceTest extends VerificationBaseTest {
  private String accountId;
  private String appId;
  private String stateExecutionId;
  private String workflowId;
  private String workflowExecutionId;
  private String serviceId;
  private String delegateTaskId;
  private Random r;
  private LogsCVConfiguration logsCVConfiguration;

  @Mock private DelegateProxyFactory delegateProxyFactory;
  @Mock private HarnessMetricRegistry metricRegistry;
  @Inject private LogAnalysisService analysisService;
  @Inject private LearningEngineService learningEngineService;
  @Inject private WingsPersistence wingsPersistence;
  private DataStoreService dataStoreService;
  private CV24x7DashboardService cv24x7DashboardService;
  private AnalysisService managerAnalysisService;
  private CVConfigurationService cvConfigurationService;
  @Mock VerificationManagerClient verificationManagerClient;

  @Before
  public void setup() throws IOException, IllegalAccessException {
    long seed = System.currentTimeMillis();
    logger.info("random seed: " + seed);
    r = new Random(seed);
    accountId = UUID.randomUUID().toString();
    appId = UUID.randomUUID().toString();
    stateExecutionId = UUID.randomUUID().toString();
    workflowId = UUID.randomUUID().toString();
    workflowExecutionId = UUID.randomUUID().toString();
    serviceId = UUID.randomUUID().toString();
    delegateTaskId = UUID.randomUUID().toString();
    MockitoAnnotations.initMocks(this);

    Call<RestResponse<Boolean>> managerCall = mock(Call.class);
    when(managerCall.execute()).thenReturn(Response.success(new RestResponse<>(true)));
    when(verificationManagerClient.isStateValid(appId, stateExecutionId)).thenReturn(managerCall);
    when(verificationManagerClient.isFeatureEnabled(FeatureName.CV_DATA_COLLECTION_JOB, accountId))
        .thenReturn(managerCall);
    setInternalState(analysisService, "managerClient", verificationManagerClient);
    setInternalState(learningEngineService, "managerClient", verificationManagerClient);
    setInternalState(analysisService, "learningEngineService", learningEngineService);

    FieldUtils.writeField(analysisService, "managerClient", verificationManagerClient, true);

    dataStoreService = new MongoDataStoreServiceImpl(wingsPersistence);
    managerAnalysisService = new AnalysisServiceImpl();
    cv24x7DashboardService = new CV24x7DashboardServiceImpl();
    cvConfigurationService = new CVConfigurationServiceImpl();
    WorkflowExecutionService workflowExecutionService = new WorkflowExecutionServiceImpl();
    FieldUtils.writeField(workflowExecutionService, "wingsPersistence", wingsPersistence, true);
    FieldUtils.writeField(managerAnalysisService, "dataStoreService", dataStoreService, true);
    FieldUtils.writeField(analysisService, "dataStoreService", dataStoreService, true);

    FieldUtils.writeField(managerAnalysisService, "wingsPersistence", wingsPersistence, true);
    FieldUtils.writeField(managerAnalysisService, "workflowExecutionService", workflowExecutionService, true);
    FieldUtils.writeField(managerAnalysisService, "metricRegistry", metricRegistry, true);
    FieldUtils.writeField(managerAnalysisService, "cv24x7DashboardService", cv24x7DashboardService, true);

    FieldUtils.writeField(cv24x7DashboardService, "wingsPersistence", wingsPersistence, true);
    FieldUtils.writeField(cv24x7DashboardService, "analysisService", managerAnalysisService, true);
    FieldUtils.writeField(cv24x7DashboardService, "cvConfigurationService", cvConfigurationService, true);

    FieldUtils.writeField(cvConfigurationService, "wingsPersistence", wingsPersistence, true);

    AnalysisContext context =
        AnalysisContext.builder().serviceId(serviceId).stateExecutionId(stateExecutionId).appId(appId).build();
    wingsPersistence.save(context);
  }

  @Test
  @Category(UnitTests.class)
  public void saveLogDataWithNoState() throws Exception {
    boolean status = analysisService.saveLogData(StateType.SPLUNKV2, accountId, appId, null, stateExecutionId,
        workflowId, workflowExecutionId, serviceId, ClusterLevel.L1, delegateTaskId,
        Collections.singletonList(new LogElement()));

    assertFalse(status);
  }

  @Test
  @Category(UnitTests.class)
  public void saveLogDataWithInvalidState() throws Exception {
    final StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setUuid(stateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.ABORTED);
    wingsPersistence.save(stateExecutionInstance);
    boolean status = analysisService.saveLogData(StateType.SPLUNKV2, accountId, appId, null, stateExecutionId,
        workflowId, workflowExecutionId, serviceId, ClusterLevel.L1, delegateTaskId,
        Collections.singletonList(new LogElement()));

    assertFalse(status);
  }

  @Test
  @Category(UnitTests.class)
  public void saveLogDataNoHeartbeat() throws Exception {
    final StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setUuid(stateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);
    boolean status = analysisService.saveLogData(StateType.SPLUNKV2, accountId, appId, null, stateExecutionId,
        workflowId, workflowExecutionId, serviceId, ClusterLevel.L1, delegateTaskId,
        Collections.singletonList(new LogElement()));

    assertFalse(status);
  }

  @Test
  @Category(UnitTests.class)
  @RealMongo
  public void saveLogDataValid() throws Exception {
    final StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setUuid(stateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);

    final List<LogElement> logElements = new ArrayList<>();

    final String query = UUID.randomUUID().toString();
    final String host = UUID.randomUUID().toString();
    final int logCollectionMinute = 3;
    LogElement splunkHeartBeatElement = new LogElement();
    splunkHeartBeatElement.setQuery(query);
    splunkHeartBeatElement.setClusterLabel("-3");
    splunkHeartBeatElement.setHost(host);
    splunkHeartBeatElement.setCount(0);
    splunkHeartBeatElement.setLogMessage("");
    splunkHeartBeatElement.setTimeStamp(0);
    splunkHeartBeatElement.setLogCollectionMinute(logCollectionMinute);

    logElements.add(splunkHeartBeatElement);

    LogElement logElement = new LogElement(query, "0", host, 0, 0, UUID.randomUUID().toString(), logCollectionMinute);
    logElements.add(logElement);

    final LogRequest logRequest = new LogRequest(
        query, appId, stateExecutionId, workflowId, serviceId, Collections.singleton(host), logCollectionMinute, false);

    Set<LogDataRecord> logDataRecords = analysisService.getLogData(
        logRequest, true, workflowExecutionId, ClusterLevel.L1, StateType.SPLUNKV2, accountId);
    assertTrue(logDataRecords.isEmpty());

    boolean status = analysisService.saveLogData(StateType.SPLUNKV2, accountId, appId, null, stateExecutionId,
        workflowId, workflowExecutionId, serviceId, ClusterLevel.L1, delegateTaskId, logElements);

    assertTrue(status);

    logDataRecords = analysisService.getLogData(
        logRequest, true, workflowExecutionId, ClusterLevel.L1, StateType.SPLUNKV2, accountId);
    assertEquals(1, logDataRecords.size());
    final LogDataRecord logDataRecord = logDataRecords.iterator().next();
    assertEquals(logElement.getLogMessage(), logDataRecord.getLogMessage());
    assertEquals(logElement.getQuery(), logDataRecord.getQuery());
    assertEquals(logElement.getClusterLabel(), logDataRecord.getClusterLabel());
    assertEquals(ClusterLevel.L1, logDataRecord.getClusterLevel());
    assertEquals(logElement.getLogCollectionMinute(), logDataRecord.getLogCollectionMinute());
  }

  @Test
  @Category(UnitTests.class)
  @RealMongo
  public void getLogDataNoSuccessfulWorkflowExecution() throws Exception {
    final StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setUuid(stateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);

    final List<LogElement> logElements = new ArrayList<>();

    final String query = UUID.randomUUID().toString();
    final String host = UUID.randomUUID().toString();
    final int logCollectionMinute = 3;
    LogElement splunkHeartBeatElement = new LogElement();
    splunkHeartBeatElement.setQuery(query);
    splunkHeartBeatElement.setClusterLabel("-3");
    splunkHeartBeatElement.setHost(host);
    splunkHeartBeatElement.setCount(0);
    splunkHeartBeatElement.setLogMessage("");
    splunkHeartBeatElement.setTimeStamp(0);
    splunkHeartBeatElement.setLogCollectionMinute(logCollectionMinute);

    logElements.add(splunkHeartBeatElement);

    LogElement logElement = new LogElement(query, "0", host, 0, 0, UUID.randomUUID().toString(), logCollectionMinute);
    logElements.add(logElement);

    final LogRequest logRequest = new LogRequest(
        query, appId, stateExecutionId, workflowId, serviceId, Collections.singleton(host), logCollectionMinute, false);

    boolean status = analysisService.saveLogData(StateType.SPLUNKV2, accountId, appId, null, stateExecutionId,
        workflowId, workflowExecutionId, serviceId, ClusterLevel.L1, delegateTaskId, logElements);

    assertTrue(status);

    Set<LogDataRecord> logData = analysisService.getLogData(
        logRequest, false, UUID.randomUUID().toString(), ClusterLevel.L1, StateType.SPLUNKV2, accountId);
    assertEquals(0, logData.size());
  }

  @Test
  @Category(UnitTests.class)
  @RealMongo
  public void getLogDataSuccessfulWorkflowExecution() throws Exception {
    final StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setUuid(stateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);

    final WorkflowExecution workflowExecution = WorkflowExecution.builder().build();
    workflowExecution.setAppId(appId);
    workflowExecution.setUuid(workflowExecutionId);
    workflowExecution.setWorkflowId(workflowId);
    workflowExecution.setStatus(ExecutionStatus.SUCCESS);
    wingsPersistence.save(workflowExecution);

    final List<LogElement> logElements = new ArrayList<>();

    final String query = UUID.randomUUID().toString();
    final String host = UUID.randomUUID().toString();
    final int logCollectionMinute = 3;
    LogElement splunkHeartBeatElement = new LogElement();
    splunkHeartBeatElement.setQuery(query);
    splunkHeartBeatElement.setClusterLabel("-3");
    splunkHeartBeatElement.setHost(host);
    splunkHeartBeatElement.setCount(0);
    splunkHeartBeatElement.setLogMessage("");
    splunkHeartBeatElement.setTimeStamp(0);
    splunkHeartBeatElement.setLogCollectionMinute(logCollectionMinute);

    logElements.add(splunkHeartBeatElement);

    LogElement logElement = new LogElement(query, "0", host, 0, 0, UUID.randomUUID().toString(), logCollectionMinute);
    logElements.add(logElement);

    final LogRequest logRequest = new LogRequest(
        query, appId, stateExecutionId, workflowId, serviceId, Collections.singleton(host), logCollectionMinute, false);

    boolean status = analysisService.saveLogData(StateType.SPLUNKV2, accountId, appId, null, stateExecutionId,
        workflowId, workflowExecutionId, serviceId, ClusterLevel.L1, delegateTaskId, logElements);

    assertTrue(status);

    Set<LogDataRecord> logDataRecords = analysisService.getLogData(
        logRequest, true, workflowExecutionId, ClusterLevel.L1, StateType.SPLUNKV2, accountId);
    assertEquals(1, logDataRecords.size());
    final LogDataRecord logDataRecord = logDataRecords.iterator().next();
    assertEquals(logElement.getLogMessage(), logDataRecord.getLogMessage());
    assertEquals(logElement.getQuery(), logDataRecord.getQuery());
    assertEquals(logElement.getClusterLabel(), logDataRecord.getClusterLabel());
    assertEquals(ClusterLevel.L1, logDataRecord.getClusterLevel());
    assertEquals(logElement.getLogCollectionMinute(), logDataRecord.getLogCollectionMinute());
  }

  @Test
  @Category(UnitTests.class)
  @RealMongo
  public void testBumpClusterLevel() throws Exception {
    final StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setUuid(stateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);

    final List<LogElement> logElements = new ArrayList<>();

    final String query = UUID.randomUUID().toString();
    final String host = UUID.randomUUID().toString();
    final int logCollectionMinute = 3;
    LogElement splunkHeartBeatElement = new LogElement();
    splunkHeartBeatElement.setQuery(query);
    splunkHeartBeatElement.setClusterLabel("-3");
    splunkHeartBeatElement.setHost(host);
    splunkHeartBeatElement.setCount(0);
    splunkHeartBeatElement.setLogMessage("");
    splunkHeartBeatElement.setTimeStamp(0);
    splunkHeartBeatElement.setLogCollectionMinute(logCollectionMinute);

    logElements.add(splunkHeartBeatElement);

    LogElement logElement = new LogElement(query, "0", host, 0, 0, UUID.randomUUID().toString(), logCollectionMinute);
    logElements.add(logElement);

    boolean status = analysisService.saveLogData(StateType.SPLUNKV2, accountId, appId, null, stateExecutionId,
        workflowId, workflowExecutionId, serviceId, ClusterLevel.L1, delegateTaskId, logElements);

    assertTrue(status);

    final LogRequest logRequest = new LogRequest(
        query, appId, stateExecutionId, workflowId, serviceId, Collections.singleton(host), logCollectionMinute, false);
    Set<LogDataRecord> logDataRecords = analysisService.getLogData(
        logRequest, true, workflowExecutionId, ClusterLevel.L1, StateType.SPLUNKV2, accountId);
    assertEquals(1, logDataRecords.size());
    LogDataRecord logDataRecord = logDataRecords.iterator().next();
    assertEquals(logElement.getLogMessage(), logDataRecord.getLogMessage());
    assertEquals(logElement.getQuery(), logDataRecord.getQuery());
    assertEquals(logElement.getClusterLabel(), logDataRecord.getClusterLabel());
    assertEquals(ClusterLevel.L1, logDataRecord.getClusterLevel());
    assertEquals(logElement.getLogCollectionMinute(), logDataRecord.getLogCollectionMinute());

    analysisService.bumpClusterLevel(StateType.SPLUNKV2, stateExecutionId, appId, query, Collections.singleton(host),
        logCollectionMinute, ClusterLevel.L1, ClusterLevel.L2);

    logDataRecords = analysisService.getLogData(
        logRequest, true, workflowExecutionId, ClusterLevel.L1, StateType.SPLUNKV2, accountId);
    assertTrue(logDataRecords.isEmpty());

    logDataRecords = analysisService.getLogData(
        logRequest, true, workflowExecutionId, ClusterLevel.L2, StateType.SPLUNKV2, accountId);
    assertEquals(1, logDataRecords.size());
    logDataRecord = logDataRecords.iterator().next();
    assertEquals(logElement.getLogMessage(), logDataRecord.getLogMessage());
    assertEquals(logElement.getQuery(), logDataRecord.getQuery());
    assertEquals(logElement.getClusterLabel(), logDataRecord.getClusterLabel());
    assertEquals(ClusterLevel.L2, logDataRecord.getClusterLevel());
    assertEquals(logElement.getLogCollectionMinute(), logDataRecord.getLogCollectionMinute());
  }

  @Test
  @Category(UnitTests.class)
  @RealMongo
  public void testIsLogDataCollected() throws Exception {
    final String query = UUID.randomUUID().toString();
    final String host = UUID.randomUUID().toString();
    final int logCollectionMinute = 3;

    assertFalse(
        analysisService.isLogDataCollected(appId, stateExecutionId, query, logCollectionMinute, StateType.SPLUNKV2));

    final StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setUuid(stateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);

    final List<LogElement> logElements = new ArrayList<>();

    LogElement splunkHeartBeatElement = new LogElement();
    splunkHeartBeatElement.setQuery(query);
    splunkHeartBeatElement.setClusterLabel("-3");
    splunkHeartBeatElement.setHost(host);
    splunkHeartBeatElement.setCount(0);
    splunkHeartBeatElement.setLogMessage("");
    splunkHeartBeatElement.setTimeStamp(0);
    splunkHeartBeatElement.setLogCollectionMinute(logCollectionMinute);

    logElements.add(splunkHeartBeatElement);

    LogElement logElement = new LogElement(query, "0", host, 0, 0, UUID.randomUUID().toString(), logCollectionMinute);
    logElements.add(logElement);

    analysisService.saveLogData(StateType.SPLUNKV2, accountId, appId, null, stateExecutionId, workflowId,
        workflowExecutionId, serviceId, ClusterLevel.L1, delegateTaskId, logElements);

    assertTrue(
        analysisService.isLogDataCollected(appId, stateExecutionId, query, logCollectionMinute, StateType.SPLUNKV2));
  }

  @Test
  @Category(UnitTests.class)
  public void shouldSaveLogCollectionMinuteMinusOne() throws Exception {
    int numOfUnknownClusters = 2 + r.nextInt(10);
    List<SplunkAnalysisCluster> clusterEvents = new ArrayList<>();
    Map<String, Map<String, SplunkAnalysisCluster>> unknownClusters = new HashMap<>();
    Set<String> hosts = new HashSet<>();
    for (int i = 0; i < numOfUnknownClusters; i++) {
      SplunkAnalysisCluster cluster = getRandomClusterEvent();
      clusterEvents.add(cluster);
      Map<String, SplunkAnalysisCluster> hostMap = new HashMap<>();
      String host = UUID.randomUUID().toString() + ".harness.com";
      hostMap.put(host, cluster);
      hosts.add(host);
      unknownClusters.put(UUID.randomUUID().toString(), hostMap);
    }

    LogMLAnalysisRecord record = new LogMLAnalysisRecord();
    record.setStateExecutionId(stateExecutionId);
    record.setAppId(appId);
    record.setStateType(StateType.SPLUNKV2);
    record.setLogCollectionMinute(-1);
    record.setAnalysisSummaryMessage("This is a -1 test");
    record.setQuery(UUID.randomUUID().toString());
    analysisService.saveLogAnalysisRecords(record, StateType.SPLUNKV2, Optional.empty());

    LogMLAnalysisSummary analysisSummary =
        managerAnalysisService.getAnalysisSummary(stateExecutionId, appId, StateType.SPLUNKV2);
    assertNotNull(analysisSummary);
    assertEquals("This is a -1 test", analysisSummary.getAnalysisSummaryMessage());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldNotSaveEmptyControlAndTestEvents() throws Exception {
    int numOfUnknownClusters = 2 + r.nextInt(10);
    List<SplunkAnalysisCluster> clusterEvents = new ArrayList<>();
    Map<String, Map<String, SplunkAnalysisCluster>> unknownClusters = new HashMap<>();
    Set<String> hosts = new HashSet<>();
    for (int i = 0; i < numOfUnknownClusters; i++) {
      SplunkAnalysisCluster cluster = getRandomClusterEvent();
      clusterEvents.add(cluster);
      Map<String, SplunkAnalysisCluster> hostMap = new HashMap<>();
      String host = UUID.randomUUID().toString() + ".harness.com";
      hostMap.put(host, cluster);
      hosts.add(host);
      unknownClusters.put(UUID.randomUUID().toString(), hostMap);
    }

    LogMLAnalysisRecord record = new LogMLAnalysisRecord();
    record.setStateExecutionId(stateExecutionId);
    record.setAppId(appId);
    record.setStateType(StateType.SPLUNKV2);
    record.setLogCollectionMinute(0);
    record.setAnalysisSummaryMessage("This is a -1 test");
    record.setQuery(UUID.randomUUID().toString());
    analysisService.saveLogAnalysisRecords(record, StateType.SPLUNKV2, Optional.empty());

    LogMLAnalysisSummary analysisSummary =
        managerAnalysisService.getAnalysisSummary(stateExecutionId, appId, StateType.SPLUNKV2);
    assertNull(analysisSummary);
  }

  @Test
  @Category(UnitTests.class)
  public void testAnalysisSummaryUnknownClusters() throws Exception {
    int numOfUnknownClusters = 2 + r.nextInt(10);
    List<SplunkAnalysisCluster> clusterEvents = new ArrayList<>();
    Map<String, Map<String, SplunkAnalysisCluster>> unknownClusters = new HashMap<>();
    Map<String, List<SplunkAnalysisCluster>> controlEvents = new HashMap<>();
    controlEvents.put("xyz", Lists.newArrayList(getRandomClusterEvent()));
    Set<String> hosts = new HashSet<>();
    for (int i = 0; i < numOfUnknownClusters; i++) {
      SplunkAnalysisCluster cluster = getRandomClusterEvent();
      clusterEvents.add(cluster);
      Map<String, SplunkAnalysisCluster> hostMap = new HashMap<>();
      String host = UUID.randomUUID().toString() + ".harness.com";
      hostMap.put(host, cluster);
      hosts.add(host);
      unknownClusters.put(UUID.randomUUID().toString(), hostMap);
    }

    LogMLAnalysisRecord record = new LogMLAnalysisRecord();
    record.setStateExecutionId(stateExecutionId);
    record.setAppId(appId);
    record.setStateType(StateType.SPLUNKV2);
    record.setLogCollectionMinute(0);
    record.setQuery(UUID.randomUUID().toString());
    record.setControl_events(controlEvents);
    record.setUnknown_clusters(unknownClusters);
    analysisService.saveLogAnalysisRecords(record, StateType.SPLUNKV2, Optional.empty());

    LogMLAnalysisSummary analysisSummary =
        managerAnalysisService.getAnalysisSummary(stateExecutionId, appId, StateType.SPLUNKV2);
    assertNotNull(analysisSummary);
    assertEquals(RiskLevel.HIGH, analysisSummary.getRiskLevel());
    assertEquals(numOfUnknownClusters, analysisSummary.getUnknownClusters().size());
    assertTrue(analysisSummary.getTestClusters().isEmpty());
    assertTrue(analysisSummary.getControlClusters().isEmpty());
    assertEquals(numOfUnknownClusters + " anomalous clusters found", analysisSummary.getAnalysisSummaryMessage());
    for (LogMLClusterSummary logMLClusterSummary : analysisSummary.getUnknownClusters()) {
      for (String hostname : logMLClusterSummary.getHostSummary().keySet()) {
        assert hosts.contains(hostname);
        hosts.remove(hostname);
      }
    }
    assert hosts.isEmpty();
  }

  @Test
  @Category(UnitTests.class)
  public void testAnalysisSummaryCompression() throws Exception {
    ArrayList<List<SplunkAnalysisCluster>> unknownEvents = Lists.newArrayList(getEvents(1 + r.nextInt(10)).values());
    Map<String, List<SplunkAnalysisCluster>> testEvents = getEvents(1 + r.nextInt(10));
    Map<String, List<SplunkAnalysisCluster>> controlEvents = getEvents(1 + r.nextInt(10));

    Map<String, Map<String, SplunkAnalysisCluster>> controlClusters = createClusters(1 + r.nextInt(10));
    Map<String, Map<String, SplunkAnalysisCluster>> unknownClusters = createClusters(1 + r.nextInt(10));
    Map<String, Map<String, SplunkAnalysisCluster>> testClusters = createClusters(1 + r.nextInt(10));
    Map<String, Map<String, SplunkAnalysisCluster>> ignoreClusters = createClusters(1 + r.nextInt(10));
    LogMLAnalysisRecord record = new LogMLAnalysisRecord();
    record.setStateExecutionId(stateExecutionId);
    record.setAppId(appId);
    record.setStateType(StateType.SPLUNKV2);
    record.setLogCollectionMinute(0);
    record.setQuery(UUID.randomUUID().toString());
    record.setUnknown_events(unknownEvents);
    record.setTest_events(testEvents);
    record.setControl_events(controlEvents);
    record.setControl_clusters(controlClusters);
    record.setUnknown_clusters(unknownClusters);
    record.setTest_clusters(testClusters);
    record.setIgnore_clusters(ignoreClusters);

    assertTrue(analysisService.saveLogAnalysisRecords(record, StateType.SPLUNKV2, Optional.empty()));

    LogMLAnalysisRecord logMLAnalysisRecord = wingsPersistence.createQuery(LogMLAnalysisRecord.class)
                                                  .filter("appId", appId)
                                                  .filter(LogMLAnalysisRecordKeys.stateExecutionId, stateExecutionId)
                                                  .get();
    assertNotNull(logMLAnalysisRecord);
    assertNull(logMLAnalysisRecord.toString(), logMLAnalysisRecord.getUnknown_events());
    assertNull(logMLAnalysisRecord.toString(), logMLAnalysisRecord.getTest_events());
    assertNull(logMLAnalysisRecord.toString(), logMLAnalysisRecord.getControl_events());
    assertNull(logMLAnalysisRecord.toString(), logMLAnalysisRecord.getControl_clusters());
    assertNull(logMLAnalysisRecord.toString(), logMLAnalysisRecord.getUnknown_clusters());
    assertNull(logMLAnalysisRecord.toString(), logMLAnalysisRecord.getTest_clusters());
    assertNull(logMLAnalysisRecord.toString(), logMLAnalysisRecord.getIgnore_clusters());
    assertTrue(isNotEmpty(logMLAnalysisRecord.getProtoSerializedAnalyisDetails()));
    assertNull(logMLAnalysisRecord.getAnalysisDetailsCompressedJson());

    LogMLAnalysisRecord logAnalysisRecord = analysisService.getLogAnalysisRecords(
        appId, stateExecutionId, record.getQuery(), record.getStateType(), record.getLogCollectionMinute());

    assertEquals(unknownEvents, logAnalysisRecord.getUnknown_events());
    assertEquals(testEvents, logAnalysisRecord.getTest_events());
    assertEquals(controlEvents, logAnalysisRecord.getControl_events());
    assertEquals(controlClusters, logAnalysisRecord.getControl_clusters());
    assertEquals(unknownClusters, logAnalysisRecord.getUnknown_clusters());
    assertEquals(testClusters, logAnalysisRecord.getTest_clusters());
    assertEquals(ignoreClusters, logAnalysisRecord.getIgnore_clusters());
  }

  @Test
  @Category(UnitTests.class)
  public void testAnalysisSummaryTestClusters() throws Exception {
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
    record.setStateExecutionId(stateExecutionId);
    record.setAppId(appId);
    record.setStateType(StateType.SPLUNKV2);
    record.setLogCollectionMinute(0);
    record.setQuery(UUID.randomUUID().toString());
    record.setControl_events(controlEvents);
    record.setTest_clusters(testClusters);
    analysisService.saveLogAnalysisRecords(record, StateType.SPLUNKV2, Optional.empty());

    int numOfUnexpectedFreq = 0;
    for (SplunkAnalysisCluster cluster : clusterEvents) {
      if (cluster.isUnexpected_freq()) {
        numOfUnexpectedFreq++;
      }
    }
    LogMLAnalysisSummary analysisSummary =
        managerAnalysisService.getAnalysisSummary(stateExecutionId, appId, StateType.SPLUNKV2);
    assertNotNull(analysisSummary);
    assertEquals(numOfUnexpectedFreq > 0 ? RiskLevel.HIGH : RiskLevel.NA, analysisSummary.getRiskLevel());
    assertEquals(numOfTestClusters, analysisSummary.getTestClusters().size());
    assertTrue(analysisSummary.getUnknownClusters().isEmpty());
    assertTrue(analysisSummary.getControlClusters().isEmpty());
    String message;
    if (numOfUnexpectedFreq == 0) {
      message = "No baseline data for the given query was found.";
    } else if (numOfUnexpectedFreq == 1) {
      message = numOfUnexpectedFreq + " anomalous cluster found";
    } else {
      message = numOfUnexpectedFreq + " anomalous clusters found";
    }
    assertEquals(message, analysisSummary.getAnalysisSummaryMessage());

    for (LogMLClusterSummary logMLClusterSummary : analysisSummary.getTestClusters()) {
      for (String hostname : logMLClusterSummary.getHostSummary().keySet()) {
        assert hosts.contains(hostname);
        hosts.remove(hostname);
      }
    }
    assert hosts.isEmpty();
  }

  @Test
  @Category(UnitTests.class)
  public void testAnalysisSummaryControlClusters() throws Exception {
    int numOfControlClusters = 1 + r.nextInt(10);
    List<SplunkAnalysisCluster> clusterEvents = new ArrayList<>();
    Map<String, Map<String, SplunkAnalysisCluster>> controlClusters = new HashMap<>();
    Set<String> hosts = new HashSet<>();
    Map<String, List<SplunkAnalysisCluster>> controlEvents = new HashMap<>();
    controlEvents.put("xyz", Lists.newArrayList(getRandomClusterEvent()));
    for (int i = 0; i < numOfControlClusters; i++) {
      SplunkAnalysisCluster cluster = getRandomClusterEvent();
      clusterEvents.add(cluster);
      Map<String, SplunkAnalysisCluster> hostMap = new HashMap<>();
      String host = UUID.randomUUID().toString() + ".harness.com";
      hostMap.put(host, cluster);
      hosts.add(host);
      controlClusters.put(UUID.randomUUID().toString(), hostMap);
    }

    LogMLAnalysisRecord record = new LogMLAnalysisRecord();
    record.setStateExecutionId(stateExecutionId);
    record.setAppId(appId);
    record.setStateType(StateType.SPLUNKV2);
    record.setLogCollectionMinute(0);
    record.setQuery(UUID.randomUUID().toString());
    record.setControl_clusters(controlClusters);
    record.setControl_events(controlEvents);
    analysisService.saveLogAnalysisRecords(record, StateType.SPLUNKV2, Optional.empty());
    LogMLAnalysisRecord logMLAnalysisRecord = wingsPersistence.createQuery(LogMLAnalysisRecord.class)
                                                  .filter("appId", appId)
                                                  .filter(LogMLAnalysisRecordKeys.stateExecutionId, stateExecutionId)
                                                  .get();
    assertNotNull(logMLAnalysisRecord);
    assertNull(logMLAnalysisRecord.getUnknown_events());
    assertNull(logMLAnalysisRecord.getTest_events());
    assertNull(logMLAnalysisRecord.getControl_events());
    assertNull(logMLAnalysisRecord.getControl_clusters());
    assertNull(logMLAnalysisRecord.getUnknown_clusters());
    assertNull(logMLAnalysisRecord.getTest_clusters());
    assertNull(logMLAnalysisRecord.getIgnore_clusters());

    LogMLAnalysisSummary analysisSummary =
        managerAnalysisService.getAnalysisSummary(stateExecutionId, appId, StateType.SPLUNKV2);
    assertNotNull(analysisSummary);
    assertEquals(RiskLevel.NA, analysisSummary.getRiskLevel());
    assertEquals(numOfControlClusters, analysisSummary.getControlClusters().size());
    assertTrue(analysisSummary.getUnknownClusters().isEmpty());
    assertTrue(analysisSummary.getTestClusters().isEmpty());
    String message = "No new data for the given queries. Showing baseline data if any.";

    assertEquals(message, analysisSummary.getAnalysisSummaryMessage());

    for (LogMLClusterSummary logMLClusterSummary : analysisSummary.getControlClusters()) {
      for (String hostname : logMLClusterSummary.getHostSummary().keySet()) {
        assert hosts.contains(hostname);
        hosts.remove(hostname);
      }
    }
    assert hosts.isEmpty();
  }

  @Test
  @Category(UnitTests.class)
  public void getCollectionMinuteForL1NoRecords() throws Exception {
    assertEquals(-1,
        analysisService.getCollectionMinuteForLevel(UUID.randomUUID().toString(), appId, stateExecutionId,
            StateType.SPLUNKV2, ClusterLevel.L1, Collections.emptySet()));
  }

  @Test
  @Category(UnitTests.class)
  public void getCollectionMinuteForL1PartialRecords() throws Exception {
    String query = UUID.randomUUID().toString();
    int numOfHosts = 2 + r.nextInt(10);
    int logCollectionMinute = 1 + r.nextInt(10);

    List<LogDataRecord> logDataRecords = new ArrayList<>();
    Set<String> hosts = new HashSet<>();
    for (int i = 0; i < numOfHosts; i++) {
      String host = UUID.randomUUID().toString();
      hosts.add(host);

      LogDataRecord logDataRecord = new LogDataRecord();
      logDataRecord.setAppId(appId);
      logDataRecord.setStateExecutionId(stateExecutionId);
      logDataRecord.setStateType(StateType.SPLUNKV2);
      logDataRecord.setClusterLevel(ClusterLevel.H1);
      logDataRecord.setQuery(query);
      logDataRecord.setLogCollectionMinute(logCollectionMinute);
      logDataRecord.setLogMessage(UUID.randomUUID().toString());
      logDataRecord.setHost(host);

      logDataRecords.add(logDataRecord);
    }

    // save all but one record

    for (int i = 1; i < numOfHosts; i++) {
      wingsPersistence.save(logDataRecords.get(i));
    }

    assertEquals(-1,
        analysisService.getCollectionMinuteForLevel(
            query, appId, stateExecutionId, StateType.SPLUNKV2, ClusterLevel.L1, hosts));
  }

  @Test
  @Category(UnitTests.class)
  public void getCollectionMinuteForL1AllRecords() throws Exception {
    String query = UUID.randomUUID().toString();
    int numOfHosts = 1 + r.nextInt(10);
    int logCollectionMinute = 1 + r.nextInt(10);

    Set<LogDataRecord> logDataRecords = new HashSet<>();
    Set<String> hosts = new HashSet<>();
    for (int i = 0; i < numOfHosts; i++) {
      String host = UUID.randomUUID().toString();
      hosts.add(host);

      LogDataRecord logDataRecord = new LogDataRecord();
      logDataRecord.setAppId(appId);
      logDataRecord.setStateExecutionId(stateExecutionId);
      logDataRecord.setStateType(StateType.SPLUNKV2);
      logDataRecord.setClusterLevel(ClusterLevel.H1);
      logDataRecord.setQuery(query);
      logDataRecord.setLogCollectionMinute(logCollectionMinute);
      logDataRecord.setLogMessage(UUID.randomUUID().toString());
      logDataRecord.setHost(host);

      logDataRecords.add(logDataRecord);
    }

    wingsPersistence.save(Lists.newArrayList(logDataRecords));

    assertEquals(logCollectionMinute,
        analysisService.getCollectionMinuteForLevel(
            query, appId, stateExecutionId, StateType.SPLUNKV2, ClusterLevel.L1, hosts));
  }

  @Test
  @Category(UnitTests.class)
  @RealMongo
  public void hasDataRecords() throws Exception {
    String query = UUID.randomUUID().toString();
    assertFalse(analysisService.hasDataRecords(
        query, appId, stateExecutionId, StateType.SPLUNKV2, Collections.singleton("some-host"), ClusterLevel.L1, 0));
    int numOfHosts = 1 + r.nextInt(10);
    int logCollectionMinute = 1 + r.nextInt(10);

    Set<LogDataRecord> logDataRecords = new HashSet<>();
    Set<String> hosts = new HashSet<>();
    for (int i = 0; i < numOfHosts; i++) {
      String host = UUID.randomUUID().toString();
      hosts.add(host);

      LogDataRecord logDataRecord = new LogDataRecord();
      logDataRecord.setAppId(appId);
      logDataRecord.setStateExecutionId(stateExecutionId);
      logDataRecord.setStateType(StateType.SPLUNKV2);
      logDataRecord.setClusterLevel(ClusterLevel.L1);
      logDataRecord.setQuery(query);
      logDataRecord.setLogCollectionMinute(logCollectionMinute);
      logDataRecord.setLogMessage(UUID.randomUUID().toString());
      logDataRecord.setHost(host);

      logDataRecords.add(logDataRecord);
    }

    wingsPersistence.save(Lists.newArrayList(logDataRecords));
    assertTrue(analysisService.hasDataRecords(
        query, appId, stateExecutionId, StateType.SPLUNKV2, hosts, ClusterLevel.L1, logCollectionMinute));
  }

  @Test
  @Category(UnitTests.class)
  public void getLogDataRecordForL0() throws Exception {
    String query = UUID.randomUUID().toString();
    assertFalse(analysisService.getHearbeatRecordForL0(appId, stateExecutionId, StateType.SPLUNKV2, null).isPresent());
    int numOfHosts = 1 + r.nextInt(10);
    int logCollectionMinute = 1 + r.nextInt(10);

    Set<LogDataRecord> logDataRecords = new HashSet<>();
    Set<String> hosts = new HashSet<>();
    for (int i = 0; i < numOfHosts; i++) {
      String host = UUID.randomUUID().toString();
      hosts.add(host);

      LogDataRecord logDataRecord = new LogDataRecord();
      logDataRecord.setAppId(appId);
      logDataRecord.setStateExecutionId(stateExecutionId);
      logDataRecord.setStateType(StateType.SPLUNKV2);
      logDataRecord.setClusterLevel(ClusterLevel.H0);
      logDataRecord.setQuery(query);
      logDataRecord.setLogCollectionMinute(logCollectionMinute);
      logDataRecord.setLogMessage(UUID.randomUUID().toString());
      logDataRecord.setHost(host);

      logDataRecords.add(logDataRecord);
    }

    wingsPersistence.save(Lists.newArrayList(logDataRecords));
    assertTrue(
        analysisService.getHearbeatRecordForL0(appId, stateExecutionId, StateType.SPLUNKV2, hosts.iterator().next())
            .isPresent());
  }

  @Test
  @Category(UnitTests.class)
  @RealMongo
  public void deleteClusterLevel() throws Exception {
    String query = UUID.randomUUID().toString();
    int numOfHosts = 1 + r.nextInt(10);
    int logCollectionMinute = 1 + r.nextInt(10);

    List<LogDataRecord> logDataRecords = new ArrayList<>();
    Set<String> hosts = new HashSet<>();
    for (int i = 0; i < numOfHosts; i++) {
      String host = UUID.randomUUID().toString();
      hosts.add(host);

      LogDataRecord logDataRecord = new LogDataRecord();
      logDataRecord.setAppId(appId);
      logDataRecord.setStateExecutionId(stateExecutionId);
      logDataRecord.setStateType(StateType.SPLUNKV2);
      logDataRecord.setClusterLevel(ClusterLevel.H0);
      logDataRecord.setQuery(query);
      logDataRecord.setLogCollectionMinute(logCollectionMinute);
      logDataRecord.setLogMessage(UUID.randomUUID().toString());
      logDataRecord.setHost(host);

      logDataRecords.add(logDataRecord);
    }

    wingsPersistence.save(logDataRecords);
    assertEquals(numOfHosts,
        wingsPersistence.createQuery(LogDataRecord.class)
            .filter(LogDataRecordKeys.stateExecutionId, stateExecutionId)
            .count());
    analysisService.deleteClusterLevel(
        StateType.SPLUNKV2, stateExecutionId, appId, query, hosts, logCollectionMinute, ClusterLevel.H0);
    assertEquals(0,
        wingsPersistence.createQuery(LogDataRecord.class)
            .filter(LogDataRecordKeys.stateExecutionId, stateExecutionId)
            .count());
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
    List<MessageFrequency> frequencyMapList = new ArrayList<>();
    for (int i = 0; i < 1 + r.nextInt(10); i++) {
      frequencyMapList.add(MessageFrequency.builder().count(r.nextInt(100)).build());
    }

    analysisCluster.setMessage_frequencies(frequencyMapList);
    return analysisCluster;
  }

  @Test
  @Category(UnitTests.class)
  public void loadPythonResponse() throws IOException {
    InputStream is = getClass().getClassLoader().getResourceAsStream("verification/LogAnalysisRecord.json");
    String jsonTxt = IOUtils.toString(is, Charset.defaultCharset());
    LogMLAnalysisRecord records = JsonUtils.asObject(jsonTxt, LogMLAnalysisRecord.class);
    assertEquals(7, records.getUnknown_events().size());
    assertEquals(33, records.getTest_events().size());
    assertEquals(31, records.getControl_events().size());
    assertEquals(31, records.getControl_clusters().size());
    assertEquals(26, records.getTest_clusters().size());
    assertEquals(4, records.getUnknown_clusters().size());
    assertEquals(0, records.getCluster_scores().getTest().size());
    assertEquals(4, records.getCluster_scores().getUnknown().size());
  }

  @Test
  @Category(UnitTests.class)
  public void checkClusterScores() throws IOException {
    InputStream is = getClass().getClassLoader().getResourceAsStream("verification/LogAnalysisRecord.json");
    String jsonTxt = IOUtils.toString(is, Charset.defaultCharset());
    LogMLAnalysisRecord records = JsonUtils.asObject(jsonTxt, LogMLAnalysisRecord.class);
    records.setStateType(ELK);
    records.setAppId(appId);
    String stateExecutionId = UUID.randomUUID().toString();
    records.setStateExecutionId(stateExecutionId);
    records.setAnalysisSummaryMessage("10");
    AnalysisContext context =
        AnalysisContext.builder().appId(appId).stateExecutionId(stateExecutionId).serviceId(serviceId).build();
    wingsPersistence.save(context);
    analysisService.saveLogAnalysisRecords(records, StateType.SPLUNKV2, Optional.empty());
    LogMLAnalysisSummary analysisSummary =
        managerAnalysisService.getAnalysisSummary(stateExecutionId, appId, StateType.SPLUNKV2);
    assertEquals(0, Double.compare(analysisSummary.getScore(), 0.23477964144180682 * 100));
    for (LogMLClusterSummary clusterSummary : analysisSummary.getUnknownClusters()) {
      assert clusterSummary.getScore() > 0;
    }
  }

  @Test
  @Category(UnitTests.class)
  public void testUserFeedback() throws Exception {
    InputStream is = getClass().getClassLoader().getResourceAsStream("verification/LogAnalysisRecord.json");
    String jsonTxt = IOUtils.toString(is, Charset.defaultCharset());
    final StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setUuid(stateExecutionId);
    stateExecutionInstance.setWorkflowId(workflowId);
    stateExecutionInstance.setStatus(ExecutionStatus.ABORTED);
    stateExecutionInstance.getContextElements().push(
        PhaseElementBuilder.aPhaseElement()
            .withServiceElement(ServiceElement.Builder.aServiceElement().withUuid(serviceId).build())
            .build());
    wingsPersistence.save(stateExecutionInstance);

    LogMLAnalysisRecord records = JsonUtils.asObject(jsonTxt, LogMLAnalysisRecord.class);
    records.setStateType(ELK);
    records.setAppId(appId);
    records.setStateExecutionId(stateExecutionId);
    records.setAnalysisSummaryMessage("10");
    analysisService.saveLogAnalysisRecords(records, ELK, Optional.empty());

    LogMLFeedback logMLFeedback = LogMLFeedback.builder()
                                      .appId(appId)
                                      .clusterLabel(0)
                                      .clusterType(AnalysisServiceImpl.CLUSTER_TYPE.UNKNOWN)
                                      .comment("excellent!!")
                                      .logMLFeedbackType(AnalysisServiceImpl.LogMLFeedbackType.IGNORE_ALWAYS)
                                      .stateExecutionId(stateExecutionId)
                                      .build();

    managerAnalysisService.saveFeedback(logMLFeedback, ELK);
    List<LogMLFeedbackRecord> mlFeedback =
        managerAnalysisService.getMLFeedback(appId, serviceId, workflowId, workflowExecutionId);
    assertFalse(mlFeedback.isEmpty());
  }

  @Test
  @Category(UnitTests.class)
  public void logQueryTrim() {
    ElkAnalysisState elkAnalysisState = new ElkAnalysisState("some name");
    elkAnalysisState.setQuery(" er ror ");
    assertEquals("er ror", elkAnalysisState.getQuery());
  }

  @Test
  @Category(UnitTests.class)
  public void formatDate() {
    ZonedDateTime zdt = ZonedDateTime.parse("2018-05-10T16:35:27.044Z");
    logger.info("" + zdt.toEpochSecond());

    zdt = ZonedDateTime.parse("2018-04-27T23:11:23.628Z");
    logger.info("" + zdt.toEpochSecond());

    DateTimeFormatter df1 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
    logger.info("" + Instant.from(df1.parse("2018-04-27T23:11:23.628Z")).toEpochMilli());

    df1 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSX");
    logger.info("" + Instant.from(df1.parse("2018-04-27T23:11:23.628456789Z")).toEpochMilli());

    DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSXXX");
    logger.info("" + Instant.from(df.parse("2018-05-03T00:15:12.618905414+00:00")).toEpochMilli());
  }

  @Test
  @Category(UnitTests.class)
  public void testReadLogMLRecordFromDB() throws Exception {
    ArrayList<List<SplunkAnalysisCluster>> unknownEvents = Lists.newArrayList(getEvents(1 + r.nextInt(10)).values());
    Map<String, List<SplunkAnalysisCluster>> testEvents = getEvents(1 + r.nextInt(10));
    Map<String, List<SplunkAnalysisCluster>> controlEvents = getEvents(1 + r.nextInt(10));

    Map<String, Map<String, SplunkAnalysisCluster>> controlClusters = createClusters(1 + r.nextInt(10));
    Map<String, Map<String, SplunkAnalysisCluster>> unknownClusters = createClusters(1 + r.nextInt(10));
    Map<String, Map<String, SplunkAnalysisCluster>> testClusters = createClusters(1 + r.nextInt(10));
    Map<String, Map<String, SplunkAnalysisCluster>> ignoreClusters = createClusters(1 + r.nextInt(10));
    LogMLAnalysisRecord record = LogMLAnalysisRecord.builder()
                                     .stateExecutionId(stateExecutionId)
                                     .appId(appId)
                                     .stateType(StateType.SPLUNKV2)
                                     .logCollectionMinute(0)
                                     .query(UUID.randomUUID().toString())
                                     .unknown_events(unknownEvents)
                                     .test_events(testEvents)
                                     .control_events(controlEvents)
                                     .control_clusters(controlClusters)
                                     .unknown_clusters(unknownClusters)
                                     .test_clusters(testClusters)
                                     .ignore_clusters(ignoreClusters)
                                     .build();
    String logAnalysisRecordId = wingsPersistence.save(record);
    LogMLAnalysisRecord savedRecord = wingsPersistence.get(LogMLAnalysisRecord.class, logAnalysisRecordId);

    assertEquals(unknownEvents, savedRecord.getUnknown_events());
    assertEquals(testEvents, savedRecord.getTest_events());
    assertEquals(controlEvents, savedRecord.getControl_events());
    assertEquals(controlClusters, savedRecord.getControl_clusters());
    assertEquals(unknownClusters, savedRecord.getUnknown_clusters());
    assertEquals(testClusters, savedRecord.getTest_clusters());
    assertEquals(ignoreClusters, savedRecord.getIgnore_clusters());
  }

  @Test
  @Category(UnitTests.class)
  public void testCleanup() {
    wingsPersistence.delete(wingsPersistence.createQuery(AnalysisContext.class)
                                .filter(AnalysisContextKeys.stateExecutionId, stateExecutionId));
    int numOfRecords = 10;
    for (int i = 0; i < numOfRecords; i++) {
      LogDataRecord logDataRecord = new LogDataRecord();
      logDataRecord.setStateExecutionId(stateExecutionId);
      logDataRecord.setAppId(appId);
      logDataRecord.setLogCollectionMinute(i);
      wingsPersistence.save(logDataRecord);
      wingsPersistence.save(
          LogMLAnalysisRecord.builder().stateExecutionId(stateExecutionId).logCollectionMinute(i).appId(appId).build());
      wingsPersistence.save(
          ContinuousVerificationExecutionMetaData.builder().stateExecutionId(stateExecutionId).build());
      wingsPersistence.save(
          LearningEngineAnalysisTask.builder().state_execution_id(stateExecutionId).analysis_minute(i).build());
      wingsPersistence.save(LearningEngineExperimentalAnalysisTask.builder()
                                .state_execution_id(stateExecutionId)
                                .analysis_minute(i)
                                .build());
      ExperimentalLogMLAnalysisRecord experimentalLogMLAnalysisRecord = new ExperimentalLogMLAnalysisRecord();
      experimentalLogMLAnalysisRecord.setStateExecutionId(stateExecutionId);
      experimentalLogMLAnalysisRecord.setLogCollectionMinute(i);
      wingsPersistence.save(experimentalLogMLAnalysisRecord);
      wingsPersistence.save(
          AnalysisContext.builder().stateExecutionId(stateExecutionId).serviceId("service-" + i).build());
    }

    assertEquals(numOfRecords, wingsPersistence.createQuery(LogDataRecord.class).count());
    assertEquals(numOfRecords, wingsPersistence.createQuery(LogMLAnalysisRecord.class).count());
    assertEquals(numOfRecords, wingsPersistence.createQuery(ContinuousVerificationExecutionMetaData.class).count());
    assertEquals(numOfRecords, wingsPersistence.createQuery(LearningEngineAnalysisTask.class).count());
    assertEquals(numOfRecords, wingsPersistence.createQuery(LearningEngineExperimentalAnalysisTask.class).count());
    assertEquals(numOfRecords, wingsPersistence.createQuery(ExperimentalLogMLAnalysisRecord.class).count());
    assertEquals(numOfRecords, wingsPersistence.createQuery(AnalysisContext.class).count());

    managerAnalysisService.cleanUpForLogRetry(stateExecutionId);
    assertEquals(0, wingsPersistence.createQuery(LogDataRecord.class).count());
    assertEquals(0, wingsPersistence.createQuery(LogMLAnalysisRecord.class).count());
    assertEquals(0, wingsPersistence.createQuery(ContinuousVerificationExecutionMetaData.class).count());
    assertEquals(0, wingsPersistence.createQuery(LearningEngineAnalysisTask.class).count());
    assertEquals(0, wingsPersistence.createQuery(LearningEngineExperimentalAnalysisTask.class).count());
    assertEquals(0, wingsPersistence.createQuery(ExperimentalLogMLAnalysisRecord.class).count());
    assertEquals(0, wingsPersistence.createQuery(AnalysisContext.class).count());
  }

  private Map<String, Map<String, SplunkAnalysisCluster>> createClusters(int numOfClusters) {
    Map<String, Map<String, SplunkAnalysisCluster>> rv = new HashMap<>();
    for (int i = 0; i < numOfClusters; i++) {
      SplunkAnalysisCluster cluster = getRandomClusterEvent();
      Map<String, SplunkAnalysisCluster> hostMap = new HashMap<>();
      String host = UUID.randomUUID().toString();
      hostMap.put(host, cluster);
      rv.put(UUID.randomUUID().toString(), hostMap);
    }
    return rv;
  }

  private Map<String, List<SplunkAnalysisCluster>> getEvents(int numOfEvents) {
    Map<String, List<SplunkAnalysisCluster>> rv = new HashMap<>();
    for (int i = 0; i < numOfEvents; i++) {
      rv.put(generateUuid(),
          Lists.newArrayList(getRandomClusterEvent(), getRandomClusterEvent(), getRandomClusterEvent()));
    }
    return rv;
  }

  private void createLogsCVConfig(boolean enabled24x7) {
    logsCVConfiguration = new LogsCVConfiguration();
    logsCVConfiguration.setName("Config 1");
    logsCVConfiguration.setAppId(appId);
    logsCVConfiguration.setEnvId(UUID.randomUUID().toString());
    logsCVConfiguration.setStateType(ELK);
    logsCVConfiguration.setServiceId(serviceId);
    logsCVConfiguration.setEnabled24x7(enabled24x7);
    logsCVConfiguration.setConnectorId(UUID.randomUUID().toString());
    logsCVConfiguration.setAnalysisTolerance(AnalysisTolerance.MEDIUM);
    logsCVConfiguration.setBaselineStartMinute(100);
    logsCVConfiguration.setBaselineEndMinute(200);

    logsCVConfiguration.setQuery("query1");
  }

  @Test
  @Category(UnitTests.class)
  public void testCompressionLogMlAnalysisRecord() throws IOException {
    File file = new File(getClass().getClassLoader().getResource("./elk/logml_data_record.json").getFile());

    final Gson gson = new Gson();
    LogMLAnalysisRecord logMLAnalysisRecord;
    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      Type type = new TypeToken<LogMLAnalysisRecord>() {}.getType();
      logMLAnalysisRecord = gson.fromJson(br, type);
    }

    assertNotNull(logMLAnalysisRecord);
    assertFalse(logMLAnalysisRecord.getUnknown_events().isEmpty());
    assertFalse(logMLAnalysisRecord.getTest_events().isEmpty());
    assertFalse(logMLAnalysisRecord.getControl_events().isEmpty());
    assertFalse(logMLAnalysisRecord.getControl_clusters().isEmpty());
    assertFalse(logMLAnalysisRecord.getUnknown_clusters().isEmpty());
    assertFalse(logMLAnalysisRecord.getTest_clusters().isEmpty());
    assertFalse(logMLAnalysisRecord.getIgnore_clusters().isEmpty());

    LogMLAnalysisRecord compressedLogMLAnalysisRecord;
    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      Type type = new TypeToken<LogMLAnalysisRecord>() {}.getType();
      compressedLogMLAnalysisRecord = gson.fromJson(br, type);
    }
    assertNotNull(compressedLogMLAnalysisRecord);

    compressedLogMLAnalysisRecord.compressLogAnalysisRecord();
    assertNotNull(compressedLogMLAnalysisRecord.getProtoSerializedAnalyisDetails());
    assertNull(compressedLogMLAnalysisRecord.getUnknown_events());
    assertNull(compressedLogMLAnalysisRecord.getTest_events());
    assertNull(compressedLogMLAnalysisRecord.getControl_events());
    assertNull(compressedLogMLAnalysisRecord.getControl_clusters());
    assertNull(compressedLogMLAnalysisRecord.getTest_clusters());
    assertNull(compressedLogMLAnalysisRecord.getUnknown_clusters());
    assertNull(compressedLogMLAnalysisRecord.getIgnore_clusters());

    compressedLogMLAnalysisRecord.decompressLogAnalysisRecord();
    assertNull(compressedLogMLAnalysisRecord.getProtoSerializedAnalyisDetails());
    assertNotNull(compressedLogMLAnalysisRecord.getUnknown_events());
    assertNotNull(compressedLogMLAnalysisRecord.getTest_events());
    assertNotNull(compressedLogMLAnalysisRecord.getControl_events());
    assertNotNull(compressedLogMLAnalysisRecord.getControl_clusters());
    assertNotNull(compressedLogMLAnalysisRecord.getTest_clusters());
    assertNotNull(compressedLogMLAnalysisRecord.getUnknown_clusters());
    assertNotNull(compressedLogMLAnalysisRecord.getIgnore_clusters());

    assertEquals(logMLAnalysisRecord.getAnalysisStatus(), compressedLogMLAnalysisRecord.getAnalysisStatus());
    assertEquals(logMLAnalysisRecord.getUnknown_events(), compressedLogMLAnalysisRecord.getUnknown_events());
    assertEquals(logMLAnalysisRecord.getTest_events(), compressedLogMLAnalysisRecord.getTest_events());
    assertEquals(logMLAnalysisRecord.getControl_events(), compressedLogMLAnalysisRecord.getControl_events());
    assertEquals(logMLAnalysisRecord.getTest_clusters(), compressedLogMLAnalysisRecord.getTest_clusters());
    assertEquals(logMLAnalysisRecord.getControl_clusters(), compressedLogMLAnalysisRecord.getControl_clusters());
    assertEquals(logMLAnalysisRecord.getUnknown_clusters(), compressedLogMLAnalysisRecord.getUnknown_clusters());
    assertEquals(logMLAnalysisRecord.getIgnore_clusters(), compressedLogMLAnalysisRecord.getIgnore_clusters());
  }

  @Test
  @Category(UnitTests.class)
  public void testCompressionLogMlAnalysisRecordOnDemand() throws IOException {
    File file = new File(getClass().getClassLoader().getResource("./elk/logml_data_record.json").getFile());

    final Gson gson = new Gson();
    LogMLAnalysisRecord logMLAnalysisRecord;
    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      Type type = new TypeToken<LogMLAnalysisRecord>() {}.getType();
      logMLAnalysisRecord = gson.fromJson(br, type);
    }

    assertNotNull(logMLAnalysisRecord);
    assertFalse(logMLAnalysisRecord.getUnknown_events().isEmpty());
    assertFalse(logMLAnalysisRecord.getTest_events().isEmpty());
    assertFalse(logMLAnalysisRecord.getControl_events().isEmpty());
    assertFalse(logMLAnalysisRecord.getControl_clusters().isEmpty());
    assertFalse(logMLAnalysisRecord.getUnknown_clusters().isEmpty());
    assertFalse(logMLAnalysisRecord.getTest_clusters().isEmpty());
    assertFalse(logMLAnalysisRecord.getIgnore_clusters().isEmpty());
    assertNull(logMLAnalysisRecord.getAnalysisDetailsCompressedJson());
    assertNull(logMLAnalysisRecord.getProtoSerializedAnalyisDetails());

    // save using json compression
    LogMLAnalysisRecord logAnalysisDetails = LogMLAnalysisRecord.builder()
                                                 .unknown_events(logMLAnalysisRecord.getUnknown_events())
                                                 .test_events(logMLAnalysisRecord.getTest_events())
                                                 .control_events(logMLAnalysisRecord.getControl_events())
                                                 .control_clusters(logMLAnalysisRecord.getControl_clusters())
                                                 .unknown_clusters(logMLAnalysisRecord.getUnknown_clusters())
                                                 .test_clusters(logMLAnalysisRecord.getTest_clusters())
                                                 .ignore_clusters(logMLAnalysisRecord.getIgnore_clusters())
                                                 .build();

    logMLAnalysisRecord.setAnalysisDetailsCompressedJson(compressString(JsonUtils.asJson(logAnalysisDetails)));
    logMLAnalysisRecord.setUnknown_events(null);
    logMLAnalysisRecord.setTest_events(null);
    logMLAnalysisRecord.setControl_events(null);
    logMLAnalysisRecord.setControl_clusters(null);
    logMLAnalysisRecord.setUnknown_clusters(null);
    logMLAnalysisRecord.setTest_clusters(null);
    logMLAnalysisRecord.setIgnore_clusters(null);
    wingsPersistence.save(logMLAnalysisRecord);

    LogMLAnalysisRecord savedMlAnalysisRecord =
        wingsPersistence.get(LogMLAnalysisRecord.class, logMLAnalysisRecord.getUuid());
    assertNull(savedMlAnalysisRecord.getUnknown_events());
    assertNull(savedMlAnalysisRecord.getTest_events());
    assertNull(savedMlAnalysisRecord.getControl_events());
    assertNull(savedMlAnalysisRecord.getControl_clusters());
    assertNull(savedMlAnalysisRecord.getUnknown_clusters());
    assertNull(savedMlAnalysisRecord.getTest_clusters());
    assertNull(savedMlAnalysisRecord.getIgnore_clusters());
    assertNull(savedMlAnalysisRecord.getProtoSerializedAnalyisDetails());
    assertNotNull(savedMlAnalysisRecord.getAnalysisDetailsCompressedJson());

    analysisService.getLogAnalysisRecords(
        logMLAnalysisRecord.getCvConfigId(), logMLAnalysisRecord.getLogCollectionMinute(), false);
    savedMlAnalysisRecord = wingsPersistence.get(LogMLAnalysisRecord.class, logMLAnalysisRecord.getUuid());
    assertNull(savedMlAnalysisRecord.getUnknown_events());
    assertNull(savedMlAnalysisRecord.getTest_events());
    assertNull(savedMlAnalysisRecord.getControl_events());
    assertNull(savedMlAnalysisRecord.getControl_clusters());
    assertNull(savedMlAnalysisRecord.getUnknown_clusters());
    assertNull(savedMlAnalysisRecord.getTest_clusters());
    assertNull(savedMlAnalysisRecord.getIgnore_clusters());
    assertNull(savedMlAnalysisRecord.getAnalysisDetailsCompressedJson());
    assertNotNull(savedMlAnalysisRecord.getProtoSerializedAnalyisDetails());

    LogMLAnalysisRecord logMLAnalysisRecordToCompare;
    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      Type type = new TypeToken<LogMLAnalysisRecord>() {}.getType();
      logMLAnalysisRecordToCompare = gson.fromJson(br, type);
    }
    assertNotNull(logMLAnalysisRecordToCompare);
    assertNotNull(logMLAnalysisRecordToCompare.getUnknown_events());
    assertNotNull(logMLAnalysisRecordToCompare.getTest_events());
    assertNotNull(logMLAnalysisRecordToCompare.getControl_events());
    assertNotNull(logMLAnalysisRecordToCompare.getControl_clusters());
    assertNotNull(logMLAnalysisRecordToCompare.getTest_clusters());
    assertNotNull(logMLAnalysisRecordToCompare.getUnknown_clusters());
    assertNotNull(logMLAnalysisRecordToCompare.getIgnore_clusters());
    assertNull(logMLAnalysisRecordToCompare.getAnalysisDetailsCompressedJson());
    assertNull(logMLAnalysisRecordToCompare.getProtoSerializedAnalyisDetails());

    logMLAnalysisRecord = analysisService.getLogAnalysisRecords(
        logMLAnalysisRecord.getCvConfigId(), logMLAnalysisRecord.getLogCollectionMinute(), true);
    assertNull(logMLAnalysisRecord.getUnknown_events());
    assertNull(logMLAnalysisRecord.getTest_events());
    assertNull(logMLAnalysisRecord.getControl_events());
    assertNull(logMLAnalysisRecord.getControl_clusters());
    assertNull(logMLAnalysisRecord.getUnknown_clusters());
    assertNull(logMLAnalysisRecord.getTest_clusters());
    assertNull(logMLAnalysisRecord.getIgnore_clusters());
    assertNull(logMLAnalysisRecord.getAnalysisDetailsCompressedJson());
    assertNotNull(logMLAnalysisRecord.getProtoSerializedAnalyisDetails());

    logMLAnalysisRecord.decompressLogAnalysisRecord();
    assertEquals(logMLAnalysisRecord.getAnalysisStatus(), logMLAnalysisRecordToCompare.getAnalysisStatus());
    assertEquals(logMLAnalysisRecord.getUnknown_events(), logMLAnalysisRecordToCompare.getUnknown_events());
    assertEquals(logMLAnalysisRecord.getTest_events(), logMLAnalysisRecordToCompare.getTest_events());
    assertEquals(logMLAnalysisRecord.getControl_events(), logMLAnalysisRecordToCompare.getControl_events());
    assertEquals(logMLAnalysisRecord.getTest_clusters(), logMLAnalysisRecordToCompare.getTest_clusters());
    assertEquals(logMLAnalysisRecord.getControl_clusters(), logMLAnalysisRecordToCompare.getControl_clusters());
    assertEquals(logMLAnalysisRecord.getUnknown_clusters(), logMLAnalysisRecordToCompare.getUnknown_clusters());
    assertEquals(logMLAnalysisRecord.getIgnore_clusters(), logMLAnalysisRecordToCompare.getIgnore_clusters());

    logMLAnalysisRecord = analysisService.getLogAnalysisRecords(
        logMLAnalysisRecord.getCvConfigId(), logMLAnalysisRecord.getLogCollectionMinute(), false);
    assertNotNull(logMLAnalysisRecord.getUnknown_events());
    assertNotNull(logMLAnalysisRecord.getTest_events());
    assertNotNull(logMLAnalysisRecord.getControl_events());
    assertNotNull(logMLAnalysisRecord.getControl_clusters());
    assertNotNull(logMLAnalysisRecord.getUnknown_clusters());
    assertNotNull(logMLAnalysisRecord.getTest_clusters());
    assertNotNull(logMLAnalysisRecord.getIgnore_clusters());
    assertNull(logMLAnalysisRecord.getAnalysisDetailsCompressedJson());
    assertNull(logMLAnalysisRecord.getProtoSerializedAnalyisDetails());

    assertEquals(logMLAnalysisRecord.getAnalysisStatus(), logMLAnalysisRecordToCompare.getAnalysisStatus());
    assertEquals(logMLAnalysisRecord.getUnknown_events(), logMLAnalysisRecordToCompare.getUnknown_events());
    assertEquals(logMLAnalysisRecord.getTest_events(), logMLAnalysisRecordToCompare.getTest_events());
    assertEquals(logMLAnalysisRecord.getControl_events(), logMLAnalysisRecordToCompare.getControl_events());
    assertEquals(logMLAnalysisRecord.getTest_clusters(), logMLAnalysisRecordToCompare.getTest_clusters());
    assertEquals(logMLAnalysisRecord.getControl_clusters(), logMLAnalysisRecordToCompare.getControl_clusters());
    assertEquals(logMLAnalysisRecord.getUnknown_clusters(), logMLAnalysisRecordToCompare.getUnknown_clusters());
    assertEquals(logMLAnalysisRecord.getIgnore_clusters(), logMLAnalysisRecordToCompare.getIgnore_clusters());
  }

  @Test
  @Category(UnitTests.class)
  public void testGetAnalysisForBaseline() throws IOException {
    File file = new File(getClass().getClassLoader().getResource("./elk/logml_data_record.json").getFile());

    final Gson gson = new Gson();
    LogMLAnalysisRecord logMLAnalysisRecord;
    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      Type type = new TypeToken<LogMLAnalysisRecord>() {}.getType();
      logMLAnalysisRecord = gson.fromJson(br, type);
    }
    LogsCVConfiguration logsCVConfiguration = new LogsCVConfiguration();
    logsCVConfiguration.setUuid(logMLAnalysisRecord.getCvConfigId());
    wingsPersistence.save(logsCVConfiguration);

    int numOfRecords = 10;
    for (int i = 0; i < numOfRecords; i++) {
      logMLAnalysisRecord.setUuid(null);
      logMLAnalysisRecord.setLogCollectionMinute(i + 1);
      wingsPersistence.save(logMLAnalysisRecord);
    }

    assertEquals(
        numOfRecords, wingsPersistence.createQuery(LogMLAnalysisRecord.class, excludeAuthority).asList().size());

    final LogMLAnalysisRecord logAnalysisRecord =
        analysisService.getLogAnalysisRecords(logMLAnalysisRecord.getCvConfigId(), numOfRecords, false);
    assertEquals(numOfRecords, logAnalysisRecord.getLogCollectionMinute());
  }

  @Test
  @Category(UnitTests.class)
  public void testSaveDuplicate() throws IOException {
    File file = new File(getClass().getClassLoader().getResource("./elk/logml_data_record.json").getFile());

    final Gson gson = new Gson();
    LogMLAnalysisRecord logMLAnalysisRecord;
    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      Type type = new TypeToken<LogMLAnalysisRecord>() {}.getType();
      logMLAnalysisRecord = gson.fromJson(br, type);
    }

    LogsCVConfiguration logsCVConfiguration = new LogsCVConfiguration();
    logsCVConfiguration.setUuid(logMLAnalysisRecord.getCvConfigId());
    wingsPersistence.save(logsCVConfiguration);

    assertEquals(0, wingsPersistence.createQuery(LogMLAnalysisRecord.class, excludeAuthority).asList().size());
    analysisService.save24X7LogAnalysisRecords(logMLAnalysisRecord.getAppId(), logMLAnalysisRecord.getCvConfigId(),
        logMLAnalysisRecord.getLogCollectionMinute(), null, logMLAnalysisRecord, Optional.empty(), Optional.empty());

    assertEquals(1, wingsPersistence.createQuery(LogMLAnalysisRecord.class, excludeAuthority).asList().size());

    analysisService.save24X7LogAnalysisRecords(logMLAnalysisRecord.getAppId(), logMLAnalysisRecord.getCvConfigId(),
        logMLAnalysisRecord.getLogCollectionMinute(), null, logMLAnalysisRecord, Optional.empty(), Optional.empty());

    assertEquals(1, wingsPersistence.createQuery(LogMLAnalysisRecord.class, excludeAuthority).asList().size());
  }

  @Test
  @Category(UnitTests.class)
  public void testServiceGuardBackoffCountNewTask() {
    // Setup with sample failed task
    int analysisMinute = 1234567;
    // test behavior
    boolean isEligible =
        learningEngineService.isEligibleToCreateTask(stateExecutionId, "", analysisMinute, MLAnalysisType.LOG_ML);
    assertTrue(isEligible);
    int nextCount = learningEngineService.getNextServiceGuardBackoffCount(
        stateExecutionId, "", analysisMinute, MLAnalysisType.LOG_ML);
    assertEquals(1, nextCount);
  }

  @Test
  @Category(UnitTests.class)
  public void testServiceGuardBackoffCount() {
    // Setup with sample failed task
    int analysisMinute = 1234567;
    createLETaskForBackoffTest(analysisMinute, 1);

    // test behavior
    boolean isEligible =
        learningEngineService.isEligibleToCreateTask(stateExecutionId, "", analysisMinute, MLAnalysisType.LOG_ML);
    assertFalse(isEligible);
  }

  @Test
  @Category(UnitTests.class)
  public void testServiceGuardMaxBackoffCount() {
    // Setup with sample failed task
    int analysisMinute = 1234567;

    createLETaskForBackoffTest(analysisMinute, 8);
    Datastore datastore = wingsPersistence.getDatastore(LearningEngineAnalysisTask.class, ReadPref.NORMAL);

    Query<LearningEngineAnalysisTask> taskQuery =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
            .filter(LearningEngineAnalysisTaskKeys.state_execution_id, stateExecutionId + "-retry-1234");
    UpdateOperations<LearningEngineAnalysisTask> updateOperations =
        wingsPersistence.createUpdateOperations(LearningEngineAnalysisTask.class)
            .set(LearningEngineAnalysisTask.LAST_UPDATED_AT_KEY,
                System.currentTimeMillis() - TimeUnit.HOURS.toMillis(4));
    datastore.update(taskQuery, updateOperations);

    // test behavior
    boolean isEligibleForTask =
        learningEngineService.isEligibleToCreateTask(stateExecutionId, "", analysisMinute, MLAnalysisType.LOG_ML);
    assertTrue(isEligibleForTask);
    int nextCount = learningEngineService.getNextServiceGuardBackoffCount(
        stateExecutionId, "", analysisMinute, MLAnalysisType.LOG_ML);

    // verify
    assertEquals(BACKOFF_LIMIT, nextCount);
  }

  private void createLETaskForBackoffTest(int analysisMinute, int backoffCount) {
    LearningEngineAnalysisTask failedTask = LearningEngineAnalysisTask.builder()
                                                .state_execution_id(stateExecutionId + "-retry-1234")
                                                .analysis_minute(analysisMinute)
                                                .service_guard_backoff_count(backoffCount)
                                                .ml_analysis_type(MLAnalysisType.LOG_ML)
                                                .build();

    failedTask.setUuid("failedUUID");

    wingsPersistence.save(failedTask);
  }
}
