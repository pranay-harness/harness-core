package software.wings.service;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.reflection.Whitebox;
import software.wings.WingsBaseTest;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.KibanaConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SplunkConfig;
import software.wings.beans.SumoConfig;
import software.wings.beans.WorkflowExecution;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.rules.RealMongo;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.LogDataRecord;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.analysis.LogRequest;
import software.wings.service.impl.elk.ElkDelegateServiceImpl;
import software.wings.service.impl.splunk.SplunkDelegateServiceImpl;
import software.wings.service.impl.sumo.SumoDelegateServiceImpl;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.elk.ElkAnalysisService;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;

/**
 * Created by rsingh on 9/27/17.
 */
public class LogMLAnalysisServiceTest extends WingsBaseTest {
  private String accountId;
  private String appId;
  private String stateExecutionId;
  private String workflowId;
  private String workflowExecutionId;
  private String serviceId;
  private String delegateTaskId;

  @Mock private DelegateProxyFactory delegateProxyFactory;
  @Inject private AnalysisService analysisService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ElkAnalysisService elkAnalysisService;

  @Before
  public void setup() {
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
  public void testValidateSplunkConfig() throws Exception {
    Mockito.when(delegateProxyFactory.get(Mockito.anyObject(), Mockito.any(SyncTaskContext.class)))
        .thenReturn(new SplunkDelegateServiceImpl());
    Whitebox.setInternalState(analysisService, "delegateProxyFactory", delegateProxyFactory);
    final SplunkConfig splunkConfig = SplunkConfig.builder()
                                          .accountId(accountId)
                                          .splunkUrl("https://ec2-52-54-103-49.compute-1.amazonaws.com:8089")
                                          .password("W!ngs@Splunk".toCharArray())
                                          .username("admin")
                                          .build();

    final SettingAttribute settingAttribute =
        SettingAttribute.Builder.aSettingAttribute().withAccountId(accountId).withValue(splunkConfig).build();
    analysisService.validateConfig(settingAttribute, StateType.SPLUNKV2);
  }

  @Test
  public void testVersion() throws Exception {
    Mockito.when(delegateProxyFactory.get(Mockito.anyObject(), Mockito.any(SyncTaskContext.class)))
        .thenReturn(new ElkDelegateServiceImpl());
    Whitebox.setInternalState(elkAnalysisService, "delegateProxyFactory", delegateProxyFactory);
    String version =
        elkAnalysisService.getVersion(accountId, "http://ec2-34-207-78-53.compute-1.amazonaws.com:5601/app/kibana");
    assertEquals("5.5.2", version);
  }

  @Test
  public void testValidateSumoLogicConfig() throws Exception {
    Mockito.when(delegateProxyFactory.get(Mockito.anyObject(), Mockito.any(SyncTaskContext.class)))
        .thenReturn(new SumoDelegateServiceImpl());
    Whitebox.setInternalState(analysisService, "delegateProxyFactory", delegateProxyFactory);
    final SumoConfig sumoConfig = new SumoConfig();
    sumoConfig.setAccountId(accountId);
    sumoConfig.setSumoUrl("https://api.us2.sumologic.com:443/api/v1/");
    sumoConfig.setAccessId("su6JaFdOBOOsnM".toCharArray());
    sumoConfig.setAccessKey("FdHRawsHERyb23fraNGxMpNg36EKCFUUEja3QRBVnU8r8NhMUnt9zeBtGuzcpNXi".toCharArray());

    final SettingAttribute settingAttribute =
        SettingAttribute.Builder.aSettingAttribute().withAccountId(accountId).withValue(sumoConfig).build();
    analysisService.validateConfig(settingAttribute, StateType.SUMO);
  }

  @Test
  @RealMongo
  public void saveLogDataWithNoState() throws Exception {
    boolean status = analysisService.saveLogData(StateType.SPLUNKV2, accountId, appId, stateExecutionId, workflowId,
        workflowExecutionId, serviceId, ClusterLevel.L1, delegateTaskId, Collections.singletonList(new LogElement()));

    Assert.assertFalse(status);
  }

  @Test
  @RealMongo
  public void saveLogDataWithInvalidState() throws Exception {
    final StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setUuid(stateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.ABORTED);
    wingsPersistence.save(stateExecutionInstance);
    boolean status = analysisService.saveLogData(StateType.SPLUNKV2, accountId, appId, stateExecutionId, workflowId,
        workflowExecutionId, serviceId, ClusterLevel.L1, delegateTaskId, Collections.singletonList(new LogElement()));

    Assert.assertFalse(status);
  }

  @Test
  @RealMongo
  public void saveLogDataNoHeartbeat() throws Exception {
    final StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setUuid(stateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);
    boolean status = analysisService.saveLogData(StateType.SPLUNKV2, accountId, appId, stateExecutionId, workflowId,
        workflowExecutionId, serviceId, ClusterLevel.L1, delegateTaskId, Collections.singletonList(new LogElement()));

    Assert.assertFalse(status);
  }

  @Test
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
        query, appId, stateExecutionId, workflowId, serviceId, Collections.singleton(host), logCollectionMinute);

    List<LogDataRecord> logDataRecords =
        analysisService.getLogData(logRequest, true, ClusterLevel.L1, StateType.SPLUNKV2);
    Assert.assertTrue(logDataRecords.isEmpty());

    boolean status = analysisService.saveLogData(StateType.SPLUNKV2, accountId, appId, stateExecutionId, workflowId,
        workflowExecutionId, serviceId, ClusterLevel.L1, delegateTaskId, logElements);

    Assert.assertTrue(status);

    logDataRecords = analysisService.getLogData(logRequest, true, ClusterLevel.L1, StateType.SPLUNKV2);
    assertEquals(1, logDataRecords.size());
    final LogDataRecord logDataRecord = logDataRecords.get(0);
    assertEquals(logElement.getLogMessage(), logDataRecord.getLogMessage());
    assertEquals(logElement.getQuery(), logDataRecord.getQuery());
    assertEquals(logElement.getClusterLabel(), logDataRecord.getClusterLabel());
    assertEquals(ClusterLevel.L1, logDataRecord.getClusterLevel());
    assertEquals(logElement.getLogCollectionMinute(), logDataRecord.getLogCollectionMinute());
  }

  @Test
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
        query, appId, stateExecutionId, workflowId, serviceId, Collections.singleton(host), logCollectionMinute);

    boolean status = analysisService.saveLogData(StateType.SPLUNKV2, accountId, appId, stateExecutionId, workflowId,
        workflowExecutionId, serviceId, ClusterLevel.L1, delegateTaskId, logElements);

    Assert.assertTrue(status);

    try {
      analysisService.getLogData(logRequest, false, ClusterLevel.L1, StateType.SPLUNKV2);
      Assert.fail("Condition check didn't fail");
    } catch (NullPointerException e) {
      assertEquals("No successful workflow execution found for workflowId: " + workflowId, e.getMessage());
    }
  }

  @Test
  @RealMongo
  public void getLogDataSuccessfulWorkflowExecution() throws Exception {
    final StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setUuid(stateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);

    final WorkflowExecution workflowExecution = new WorkflowExecution();
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
        query, appId, stateExecutionId, workflowId, serviceId, Collections.singleton(host), logCollectionMinute);

    boolean status = analysisService.saveLogData(StateType.SPLUNKV2, accountId, appId, stateExecutionId, workflowId,
        workflowExecutionId, serviceId, ClusterLevel.L1, delegateTaskId, logElements);

    Assert.assertTrue(status);

    List<LogDataRecord> logDataRecords =
        analysisService.getLogData(logRequest, true, ClusterLevel.L1, StateType.SPLUNKV2);
    assertEquals(1, logDataRecords.size());
    final LogDataRecord logDataRecord = logDataRecords.get(0);
    assertEquals(logElement.getLogMessage(), logDataRecord.getLogMessage());
    assertEquals(logElement.getQuery(), logDataRecord.getQuery());
    assertEquals(logElement.getClusterLabel(), logDataRecord.getClusterLabel());
    assertEquals(ClusterLevel.L1, logDataRecord.getClusterLevel());
    assertEquals(logElement.getLogCollectionMinute(), logDataRecord.getLogCollectionMinute());
  }

  @Test
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

    boolean status = analysisService.saveLogData(StateType.SPLUNKV2, accountId, appId, stateExecutionId, workflowId,
        workflowExecutionId, serviceId, ClusterLevel.L1, delegateTaskId, logElements);

    Assert.assertTrue(status);

    final LogRequest logRequest = new LogRequest(
        query, appId, stateExecutionId, workflowId, serviceId, Collections.singleton(host), logCollectionMinute);
    List<LogDataRecord> logDataRecords =
        analysisService.getLogData(logRequest, true, ClusterLevel.L1, StateType.SPLUNKV2);
    assertEquals(1, logDataRecords.size());
    LogDataRecord logDataRecord = logDataRecords.get(0);
    assertEquals(logElement.getLogMessage(), logDataRecord.getLogMessage());
    assertEquals(logElement.getQuery(), logDataRecord.getQuery());
    assertEquals(logElement.getClusterLabel(), logDataRecord.getClusterLabel());
    assertEquals(ClusterLevel.L1, logDataRecord.getClusterLevel());
    assertEquals(logElement.getLogCollectionMinute(), logDataRecord.getLogCollectionMinute());

    analysisService.bumpClusterLevel(StateType.SPLUNKV2, stateExecutionId, appId, query, Collections.singleton(host),
        logCollectionMinute, ClusterLevel.L1, ClusterLevel.L2);

    logDataRecords = analysisService.getLogData(logRequest, true, ClusterLevel.L1, StateType.SPLUNKV2);
    Assert.assertTrue(logDataRecords.isEmpty());

    logDataRecords = analysisService.getLogData(logRequest, true, ClusterLevel.L2, StateType.SPLUNKV2);
    assertEquals(1, logDataRecords.size());
    logDataRecord = logDataRecords.get(0);
    assertEquals(logElement.getLogMessage(), logDataRecord.getLogMessage());
    assertEquals(logElement.getQuery(), logDataRecord.getQuery());
    assertEquals(logElement.getClusterLabel(), logDataRecord.getClusterLabel());
    assertEquals(ClusterLevel.L2, logDataRecord.getClusterLevel());
    assertEquals(logElement.getLogCollectionMinute(), logDataRecord.getLogCollectionMinute());
  }

  @Test
  @RealMongo
  public void testIsLogDataCollected() throws Exception {
    final String query = UUID.randomUUID().toString();
    final String host = UUID.randomUUID().toString();
    final int logCollectionMinute = 3;

    Assert.assertFalse(
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

    analysisService.saveLogData(StateType.SPLUNKV2, accountId, appId, stateExecutionId, workflowId, workflowExecutionId,
        serviceId, ClusterLevel.L1, delegateTaskId, logElements);

    Assert.assertTrue(
        analysisService.isLogDataCollected(appId, stateExecutionId, query, logCollectionMinute, StateType.SPLUNKV2));
  }

  @Test
  @RealMongo
  public void testIsBaseLineCreatedWithCurrentStrategy() throws Exception {
    Assert.assertTrue(analysisService.isBaselineCreated(
        AnalysisComparisonStrategy.COMPARE_WITH_CURRENT, null, null, null, null, null, null));
  }

  @Test
  @RealMongo
  public void testIsBaseLineCreatedNoWorkFlowExecutions() throws Exception {
    Assert.assertFalse(analysisService.isBaselineCreated(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS,
        StateType.SPLUNKV2, appId, workflowId, workflowExecutionId, serviceId, null));
  }

  @Test
  @RealMongo
  public void testIsBaseLineCreatedNoRecords() throws Exception {
    final WorkflowExecution workflowExecution = new WorkflowExecution();
    workflowExecution.setAppId(appId);
    workflowExecution.setWorkflowId(workflowId);
    workflowExecution.setStatus(ExecutionStatus.SUCCESS);
    wingsPersistence.save(workflowExecution);
    Assert.assertFalse(analysisService.isBaselineCreated(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS,
        StateType.SPLUNKV2, appId, workflowId, workflowExecutionId, serviceId, null));
  }

  @Test
  @RealMongo
  public void testIsBaseLineCreatedNoSuccessfulExecution() throws Exception {
    final WorkflowExecution workflowExecution = new WorkflowExecution();
    workflowExecution.setAppId(appId);
    workflowExecution.setWorkflowId(workflowId);
    workflowExecution.setStatus(ExecutionStatus.FAILED);
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
    analysisService.saveLogData(StateType.SPLUNKV2, accountId, appId, stateExecutionId, workflowId, workflowExecutionId,
        serviceId, ClusterLevel.L1, delegateTaskId, logElements);

    Assert.assertFalse(analysisService.isBaselineCreated(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS,
        StateType.SPLUNKV2, appId, workflowId, workflowExecutionId, serviceId, null));
  }

  @Test
  @RealMongo
  public void testIsBaseLineCreate() throws Exception {
    final StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setUuid(stateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);

    final WorkflowExecution workflowExecution = new WorkflowExecution();
    workflowExecution.setAppId(appId);
    workflowExecution.setWorkflowId(workflowId);
    workflowExecution.setStatus(ExecutionStatus.SUCCESS);
    workflowExecutionId = wingsPersistence.save(workflowExecution);

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
    Assert.assertTrue(analysisService.saveLogData(StateType.SPLUNKV2, accountId, appId, stateExecutionId, workflowId,
        workflowExecutionId, serviceId, ClusterLevel.L1, delegateTaskId, logElements));

    Assert.assertTrue(analysisService.isBaselineCreated(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS,
        StateType.SPLUNKV2, appId, workflowId, workflowExecutionId, serviceId, query));
  }
}
