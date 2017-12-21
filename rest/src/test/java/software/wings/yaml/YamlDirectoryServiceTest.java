package software.wings.yaml;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;

import com.google.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.ServiceCommand;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.yaml.YamlDirectoryService;

import java.util.UUID;

/**
 * Created by bsollish on 9/28/17.
 */
public class YamlDirectoryServiceTest extends WingsBaseTest {
  private String accountId;

  @Inject @InjectMocks private YamlDirectoryService yamlDirectoryService;

  // create mocks
  private static final AppService appService = mock(AppService.class);
  private static final ServiceResourceService serviceResourceService = mock(ServiceResourceService.class);
  private static final EnvironmentService environmentService = mock(EnvironmentService.class);
  private static final SettingsService settingsService = mock(SettingsService.class);

  private final String YAML_EXTENSION = ".yaml";
  private final String TEST_ACCOUNT_ID = "TEST-ACCOUNT-ID";
  private final String TEST_APP_ID = "TEST-APP-ID";
  private final String TEST_SERVICE_ID = "TEST-SERVICE-ID";
  private final String TEST_SERVICE_COMMAND_ID = "TEST-SERVICE-COMMAND-ID";
  private final String TEST_ENVIRONMENT_ID = "TEST-ENVIRONMENT-ID";
  private final String TEST_AWS_ID = "TEST-AWS-ID";
  private final String TEST_APP_NAME = "Test App";
  private final String TEST_SERVICE_NAME = "Test Service";
  private final String TEST_SERVICE_COMMAND_NAME = "Test Service Command";
  private final String TEST_ENVIRONMENT_NAME = "Test Environment";
  private final String TEST_AWS_NAME = "Test AWS";
  private final Application testApp =
      anApplication().withUuid(TEST_APP_ID).withAppId(TEST_APP_ID).withName(TEST_APP_NAME).build();
  private final Service testService =
      Service.Builder.aService().withAppId(TEST_APP_ID).withUuid(TEST_SERVICE_ID).withName(TEST_SERVICE_NAME).build();
  private final ServiceCommand testServiceCommand = ServiceCommand.Builder.aServiceCommand()
                                                        .withAppId(TEST_APP_ID)
                                                        .withServiceId(TEST_SERVICE_ID)
                                                        .withUuid(TEST_SERVICE_COMMAND_ID)
                                                        .withName(TEST_SERVICE_COMMAND_NAME)
                                                        .build();
  private final Environment testEnvironment = Environment.Builder.anEnvironment()
                                                  .withAppId(TEST_APP_ID)
                                                  .withUuid(TEST_ENVIRONMENT_ID)
                                                  .withName(TEST_ENVIRONMENT_NAME)
                                                  .build();
  private final SettingAttribute testAws = SettingAttribute.Builder.aSettingAttribute()
                                               .withAppId(TEST_APP_ID)
                                               .withUuid(TEST_AWS_ID)
                                               .withName(TEST_AWS_NAME)
                                               .build();

  @Before
  public void setup() {
    accountId = UUID.randomUUID().toString();

    when(appService.getAppByName(TEST_ACCOUNT_ID, TEST_APP_NAME)).thenReturn(testApp);
    when(serviceResourceService.getServiceByName(TEST_ACCOUNT_ID, TEST_SERVICE_NAME)).thenReturn(testService);
    when(serviceResourceService.getCommandByName(TEST_APP_ID, TEST_SERVICE_ID, TEST_SERVICE_COMMAND_NAME))
        .thenReturn(testServiceCommand);
    when(environmentService.getEnvironmentByName(TEST_ACCOUNT_ID, TEST_ENVIRONMENT_NAME)).thenReturn(testEnvironment);
    when(settingsService.getSettingAttributeByName(TEST_ACCOUNT_ID, TEST_AWS_NAME)).thenReturn(testAws);
  }

  /**
   * Tear down.
   */
  @After
  public void tearDown() {
    // we have to reset the mock after each test because of the
    // @ClassRule, or use a @Rule as mentioned below.
    reset(appService, serviceResourceService, environmentService, settingsService);
  }

  /*
  @Test
  @RealMongo
  public void getEmptyDirectory() throws Exception {
    DirectoryNode dn = yamlDirectoryService.getDirectory(accountId);

    Assert.assertNotNull(dn);
  }
  */

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