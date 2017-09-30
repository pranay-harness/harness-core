package software.wings.yaml;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.rules.RealMongo;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.yaml.directory.DirectoryNode;

import java.util.UUID;
import javax.inject.Inject;

/**
 * Created by bsollish on 9/28/17.
 */
public class YamlDirectoryServiceTest extends WingsBaseTest {
  private String accountId;

  @Inject private YamlDirectoryService yamlDirectoryService;

  @Before
  public void setup() {
    accountId = UUID.randomUUID().toString();
  }

  @Test
  @RealMongo
  public void getEmptyDirectory() throws Exception {
    DirectoryNode dn = yamlDirectoryService.getDirectory(accountId);

    Assert.assertNotNull(dn);
  }

  /* NOTE: This is copied from LogMLAnalysisServiceTest for reference using RealMongo
  @Test
  @RealMongo
  public void saveLogDataWithNoState() throws Exception {
    boolean status = analysisService
        .saveLogData(StateType.SPLUNKV2, accountId, appId, stateExecutionId, workflowId, workflowExecutionId, serviceId,
  ClusterLevel.L1, delegateTaskId, Collections.singletonList(new LogElement()));

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
    boolean status = analysisService
        .saveLogData(StateType.SPLUNKV2, accountId, appId, stateExecutionId, workflowId, workflowExecutionId, serviceId,
  ClusterLevel.L1, delegateTaskId, Collections.singletonList(new LogElement()));

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
    boolean status = analysisService
        .saveLogData(StateType.SPLUNKV2, accountId, appId, stateExecutionId, workflowId, workflowExecutionId, serviceId,
  ClusterLevel.L1, delegateTaskId, Collections.singletonList(new LogElement()));

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

    final LogRequest logRequest = new LogRequest(query, appId, stateExecutionId, workflowId, serviceId,
  Collections.singleton(host), logCollectionMinute);

    List<LogDataRecord> logDataRecords = analysisService.getLogData(logRequest, true, ClusterLevel.L1,
  StateType.SPLUNKV2); Assert.assertTrue(logDataRecords.isEmpty());

    boolean status = analysisService
        .saveLogData(StateType.SPLUNKV2, accountId, appId, stateExecutionId, workflowId, workflowExecutionId, serviceId,
  ClusterLevel.L1, delegateTaskId, logElements);

    Assert.assertTrue(status);

    logDataRecords = analysisService.getLogData(logRequest, true, ClusterLevel.L1, StateType.SPLUNKV2);
    Assert.assertEquals(1, logDataRecords.size());
    final LogDataRecord logDataRecord = logDataRecords.get(0);
    Assert.assertEquals(logElement.getLogMessage(), logDataRecord.getLogMessage());
    Assert.assertEquals(logElement.getQuery(), logDataRecord.getQuery());
    Assert.assertEquals(logElement.getClusterLabel(), logDataRecord.getClusterLabel());
    Assert.assertEquals(ClusterLevel.L1, logDataRecord.getClusterLevel());
    Assert.assertEquals(logElement.getLogCollectionMinute(), logDataRecord.getLogCollectionMinute());
  }

  @Test
  @RealMongo
  public void testIsLogDataCollected() throws Exception {
    final String query = UUID.randomUUID().toString();
    final String host = UUID.randomUUID().toString();
    final int logCollectionMinute = 3;

    Assert.assertFalse(analysisService.isLogDataCollected(appId, stateExecutionId, query, logCollectionMinute,
  StateType.SPLUNKV2));

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

    analysisService
        .saveLogData(StateType.SPLUNKV2, accountId, appId, stateExecutionId, workflowId, workflowExecutionId, serviceId,
  ClusterLevel.L1, delegateTaskId, logElements);

    Assert.assertTrue(analysisService.isLogDataCollected(appId, stateExecutionId, query, logCollectionMinute,
  StateType.SPLUNKV2));
  }

  @Test
  @RealMongo
  public void testIsBaseLineCreatedWithCurrentStrategy() throws Exception {
    Assert.assertTrue(analysisService.isBaselineCreated(AnalysisComparisonStrategy.COMPARE_WITH_CURRENT, null, null,
  null, null, null, null));
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
    analysisService
        .saveLogData(StateType.SPLUNKV2, accountId, appId, stateExecutionId, workflowId, workflowExecutionId, serviceId,
  ClusterLevel.L1, delegateTaskId, logElements);

    Assert.assertFalse(analysisService
        .isBaselineCreated(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS, StateType.SPLUNKV2, appId, workflowId,
  workflowExecutionId, serviceId, null));
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
    Assert.assertTrue(analysisService
        .saveLogData(StateType.SPLUNKV2, accountId, appId, stateExecutionId, workflowId, workflowExecutionId, serviceId,
  ClusterLevel.L1, delegateTaskId, logElements));

    Assert.assertTrue(analysisService
        .isBaselineCreated(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS, StateType.SPLUNKV2, appId, workflowId,
  workflowExecutionId, serviceId, query));
  }
  */
}