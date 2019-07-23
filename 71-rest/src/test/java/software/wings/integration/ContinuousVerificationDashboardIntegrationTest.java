package software.wings.integration;

import static org.apache.cxf.ws.addressing.ContextUtils.generateUUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.category.element.IntegrationTests;
import io.harness.rest.RestResponse;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.beans.FeatureName;
import software.wings.beans.Service;
import software.wings.beans.User;
import software.wings.beans.WorkflowExecution;
import software.wings.security.AppPermissionSummary;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.UserThreadLocal;
import software.wings.service.impl.analysis.CVDeploymentData;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData;
import software.wings.service.impl.analysis.ContinuousVerificationService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.UserService;
import software.wings.sm.PipelineSummary;
import software.wings.sm.StateType;
import software.wings.utils.WingsIntegrationTestConstants;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

public class ContinuousVerificationDashboardIntegrationTest extends BaseIntegrationTest {
  @Inject ContinuousVerificationService continuousVerificationService;
  @Inject UserService userService;
  @Inject AuthService authService;
  @Inject AppService appService;
  @Mock FeatureFlagService featureFlagService;

  private String appId;
  private String stateExecutionId;
  private String workflowId;
  private String workflowExecutionId;
  private String serviceId;
  private String envId;

  @Before
  public void setUp() throws Exception {
    loginAdminUser();
    User user = userService.getUserByEmail(WingsIntegrationTestConstants.adminUserEmail);
    UserThreadLocal.set(user);
    stateExecutionId = UUID.randomUUID().toString();

    envId = UUID.randomUUID().toString();

    workflowExecutionId = UUID.randomUUID().toString();

    Map<String, AppPermissionSummary> appsMap =
        authService.getUserPermissionInfo(accountId, user, false).getAppPermissionMapInternal();

    // we need to user the testApplication, it has workflows defined under it by Datagen.
    appId = appService.getAppByName(accountId, "Test Application").getUuid();
    serviceId = appsMap.get(appId).getServicePermissions().get(Action.READ).iterator().next();
    workflowId = appsMap.get(appId).getWorkflowPermissions().get(Action.READ).iterator().next();
    FieldUtils.writeField(continuousVerificationService, "featureFlagService", featureFlagService, true);
  }

  private void saveExecutions() {
    long now = System.currentTimeMillis();
    continuousVerificationService.saveCVExecutionMetaData(ContinuousVerificationExecutionMetaData.builder()
                                                              .accountId(accountId)
                                                              .applicationId(appId)
                                                              .appName("dummy")
                                                              .artifactName("cv dummy artifact")
                                                              .envName("cv dummy env")
                                                              .phaseName("dummy phase")
                                                              .pipelineName("dummy pipeline")
                                                              .workflowName("dummy workflow")
                                                              .pipelineStartTs(now)
                                                              .workflowStartTs(now)
                                                              .serviceId(serviceId)
                                                              .serviceName("dummy service")
                                                              .stateType(StateType.ELK)
                                                              .workflowId(workflowId)
                                                              .workflowExecutionId(workflowExecutionId)
                                                              .build());

    continuousVerificationService.saveCVExecutionMetaData(ContinuousVerificationExecutionMetaData.builder()
                                                              .accountId(accountId)
                                                              .applicationId(appId)
                                                              .appName("dummy")
                                                              .artifactName("cv dummy artifact")
                                                              .envName("cv dummy env")
                                                              .phaseName("dummy phase")
                                                              .pipelineName("dummy pipeline")
                                                              .workflowName("dummy workflow")
                                                              .pipelineStartTs(now)
                                                              .workflowStartTs(now)
                                                              .serviceId(serviceId)
                                                              .serviceName("dummy service")
                                                              .stateType(StateType.NEW_RELIC)
                                                              .workflowId(workflowId)
                                                              .workflowExecutionId(workflowExecutionId)
                                                              .build());

    continuousVerificationService.saveCVExecutionMetaData(ContinuousVerificationExecutionMetaData.builder()
                                                              .accountId(accountId + "123")
                                                              .applicationId(appId)
                                                              .appName("dummy")
                                                              .artifactName("cv dummy artifact")
                                                              .envName("cv dummy env")
                                                              .phaseName("dummy phase")
                                                              .pipelineName("dummy pipeline")
                                                              .workflowName("dummy workflow")
                                                              .pipelineStartTs(now)
                                                              .workflowStartTs(now)
                                                              .serviceId(serviceId)
                                                              .serviceName("dummy service")
                                                              .stateType(StateType.NEW_RELIC)
                                                              .workflowId(workflowId + "123")
                                                              .workflowExecutionId(workflowExecutionId)
                                                              .build());
  }

  @Test
  @Category(IntegrationTests.class)
  public void getRecords() throws Exception {
    saveExecutions();
    long now = System.currentTimeMillis();

    long before = now - TimeUnit.MINUTES.toMillis(1), after = now + TimeUnit.MINUTES.toMillis(5);
    WebTarget getTarget = client.target(
        API_BASE + "/cvdash/get-records?accountId=" + accountId + "&beginEpochTs=" + before + "&endEpochTs=" + after);

    RestResponse<Map<Long,
        TreeMap<String, Map<String, Map<String, Map<String, List<ContinuousVerificationExecutionMetaData>>>>>>>
        response = getRequestBuilderWithAuthHeader(getTarget).get(new GenericType<RestResponse<Map<Long,
                TreeMap<String,
                    Map<String, Map<String, Map<String, List<ContinuousVerificationExecutionMetaData>>>>>>>>() {});

    assertFalse("not empty resource" + response.getResource(), response.getResource().isEmpty());

    long start = Instant.ofEpochMilli(now).truncatedTo(ChronoUnit.DAYS).toEpochMilli();

    Map<Long, TreeMap<String, Map<String, Map<String, Map<String, List<ContinuousVerificationExecutionMetaData>>>>>>
        map = response.getResource();
    List<ContinuousVerificationExecutionMetaData> cvList = map.get(start)
                                                               .get("cv dummy artifact")
                                                               .get("cv dummy env/dummy workflow")
                                                               .values()
                                                               .iterator()
                                                               .next()
                                                               .get("dummy phase");
    ContinuousVerificationExecutionMetaData continuousVerificationExecutionMetaData1 = cvList.get(0);
    assertEquals(continuousVerificationExecutionMetaData1.getAccountId(), accountId);
    assertEquals(continuousVerificationExecutionMetaData1.getArtifactName(), "cv dummy artifact");

    // validate it doesnt contain info from other account.
    for (ContinuousVerificationExecutionMetaData cv : cvList) {
      assertFalse("We should not get executions of second account", cv.getAccountId().equals(accountId + "123"));
    }
  }

  @Test
  @Category(IntegrationTests.class)
  public void getAllCVDeploymentRecords() {
    // Setup
    long now = System.currentTimeMillis();
    continuousVerificationService.saveCVExecutionMetaData(ContinuousVerificationExecutionMetaData.builder()
                                                              .accountId(accountId)
                                                              .applicationId(appId)
                                                              .appName("dummy")
                                                              .artifactName("cv dummy artifact")
                                                              .envName("cv dummy env")
                                                              .phaseName("dummy phase")
                                                              .pipelineName("dummy pipeline")
                                                              .workflowName("dummy workflow")
                                                              .pipelineStartTs(now)
                                                              .workflowStartTs(now)
                                                              .serviceId(serviceId)
                                                              .stateExecutionId(stateExecutionId)
                                                              .envId(envId)
                                                              .serviceName("dummy service")
                                                              .stateType(StateType.ELK)
                                                              .workflowId(workflowId)
                                                              .workflowExecutionId(workflowExecutionId)
                                                              .build());

    WorkflowExecution execution1 =
        WorkflowExecution.builder().appId(appId).uuid(workflowExecutionId).status(ExecutionStatus.SUCCESS).build();
    wingsPersistence.save(execution1);

    // Call

    long before = now - TimeUnit.MINUTES.toMillis(1), after = now + TimeUnit.MINUTES.toMillis(5);
    List<CVDeploymentData> workflowExecutionList = continuousVerificationService.getCVDeploymentData(
        accountId, before, after, userService.getUserByEmail(WingsIntegrationTestConstants.adminUserEmail), serviceId);

    // Verify
    assertTrue("There's atleast one cv deployment execution", workflowExecutionList.size() > 0);
    assertEquals("ExecutionId matches", workflowExecutionId, workflowExecutionList.get(0).getWorkflowExecutionId());
    assertEquals("Status is success", ExecutionStatus.SUCCESS, workflowExecutionList.get(0).getStatus());
  }

  @Test
  @Category(IntegrationTests.class)
  public void getAllDeploymentRecords() {
    // Setup
    long now = System.currentTimeMillis();

    WorkflowExecution execution1 =
        WorkflowExecution.builder()
            .appId(appId)
            .uuid(workflowExecutionId)
            .envId(envId)
            .status(ExecutionStatus.SUCCESS)
            .startTs(now)
            .serviceIds(Arrays.asList(serviceId))
            .pipelineSummary(PipelineSummary.builder().pipelineId("pipelineId").pipelineName("pipelineName").build())
            .build();
    wingsPersistence.save(execution1);

    Service service = Service.builder().appId(appId).name(generateUUID()).uuid(serviceId).build();
    wingsPersistence.save(service);
    // Call

    long before = now - TimeUnit.MINUTES.toMillis(1), after = now + TimeUnit.MINUTES.toMillis(5);
    List<WorkflowExecution> workflowExecutionList = continuousVerificationService.getDeploymentsForService(
        accountId, before, after, userService.getUserByEmail(WingsIntegrationTestConstants.adminUserEmail), serviceId);

    // Verify
    boolean executionFound = false;
    assertTrue("There's atleast one deployment execution", workflowExecutionList.size() > 0);
    for (WorkflowExecution execution : workflowExecutionList) {
      if (execution.getUuid().equals(workflowExecutionId)) {
        executionFound = true;
        assertEquals("Status is success", ExecutionStatus.SUCCESS, execution.getStatus());
        assertEquals("pipeline id matches", "pipelineId", execution.getPipelineSummary().getPipelineId());
        assertEquals("pipeline name matches", "pipelineName", execution.getPipelineSummary().getPipelineName());
        assertEquals("EnvID should match", envId, execution.getEnvId());
      }
    }
    assertTrue("Workflow execution should be in the returned list", executionFound);
  }

  @Test
  @Category(IntegrationTests.class)
  public void getAllDeploymentRecordsWFWithoutServiceIds() {
    // Setup
    long now = System.currentTimeMillis();

    WorkflowExecution execution1 =
        WorkflowExecution.builder()
            .appId(appId)
            .envId(envId)
            .uuid(workflowExecutionId)
            .status(ExecutionStatus.SUCCESS)
            .startTs(now)
            .serviceIds(Arrays.asList(serviceId))
            .pipelineSummary(PipelineSummary.builder().pipelineId("pipelineId").pipelineName("pipelineName").build())
            .build();
    wingsPersistence.save(execution1);

    WorkflowExecution execution2 =
        WorkflowExecution.builder()
            .appId(appId)
            .envId(envId)
            .uuid(workflowExecutionId + "2")
            .status(ExecutionStatus.SUCCESS)
            .startTs(now)
            .pipelineSummary(PipelineSummary.builder().pipelineId("pipelineId").pipelineName("pipelineName").build())
            .build();
    wingsPersistence.save(execution2);

    Service service = Service.builder().appId(appId).uuid(serviceId).build();
    wingsPersistence.save(service);
    // Call

    long before = now - TimeUnit.MINUTES.toMillis(1), after = now + TimeUnit.MINUTES.toMillis(5);
    List<WorkflowExecution> workflowExecutionList = continuousVerificationService.getDeploymentsForService(
        accountId, before, after, userService.getUserByEmail(WingsIntegrationTestConstants.adminUserEmail), serviceId);

    // Verify
    assertTrue("There's atleast one deployment execution", workflowExecutionList.size() > 0);
    assertEquals("ExecutionId matches", workflowExecutionId, workflowExecutionList.get(0).getUuid());
    assertEquals("Status is success", ExecutionStatus.SUCCESS, workflowExecutionList.get(0).getStatus());
    assertEquals(
        "pipeline id matches", "pipelineId", workflowExecutionList.get(0).getPipelineSummary().getPipelineId());
    assertEquals(
        "pipeline name matches", "pipelineName", workflowExecutionList.get(0).getPipelineSummary().getPipelineName());
    assertEquals("EnvId exists and matches", envId, workflowExecutionList.get(0).getEnvId());
  }

  @Test
  @Category(IntegrationTests.class)
  public void getAllCVRecordsHarnessAccount() {
    saveExecutions();
    when(featureFlagService.isEnabled(FeatureName.GLOBAL_CV_DASH, accountId)).thenReturn(true);
    long now = System.currentTimeMillis();

    long before = now - TimeUnit.MINUTES.toMillis(1), after = now + TimeUnit.MINUTES.toMillis(5);

    PageResponse<ContinuousVerificationExecutionMetaData> cvList =
        continuousVerificationService.getAllCVExecutionsForTime(
            accountId, before, after, true, PageRequestBuilder.aPageRequest().build());
    assertTrue("There's atleast one cv metrics execution", cvList.size() >= 2);
    // verify if both the accounts we put in are present
    boolean account1Present = false, account2Present = false;
    for (ContinuousVerificationExecutionMetaData cvData : cvList) {
      if (cvData.getAccountId().equals(accountId)) {
        account1Present = true;
      }
      if (cvData.getAccountId().equals(accountId + "123")) {
        account2Present = true;
      }
    }
    assertTrue("We should get executions from both accounts", account1Present && account2Present);
    PageResponse<ContinuousVerificationExecutionMetaData> cvLogsList =
        continuousVerificationService.getAllCVExecutionsForTime(
            accountId, before, after, true, PageRequestBuilder.aPageRequest().build());
    assertTrue("There's atleast one cv logs execution", cvLogsList.size() > 0);
  }

  @Test
  @Category(IntegrationTests.class)
  public void getAllCVRecordsNonHarnessAccount() {
    saveExecutions();
    when(featureFlagService.isEnabled(FeatureName.GLOBAL_CV_DASH, "badAccount")).thenReturn(false);
    long now = System.currentTimeMillis();

    long before = now - TimeUnit.MINUTES.toMillis(1), after = now + TimeUnit.MINUTES.toMillis(5);

    PageResponse<ContinuousVerificationExecutionMetaData> cvList =
        continuousVerificationService.getAllCVExecutionsForTime(
            "badAccount", before, after, true, PageRequestBuilder.aPageRequest().build());
    assertTrue("There's no cv metrics execution", cvList.size() == 0);

    PageResponse<ContinuousVerificationExecutionMetaData> cvLogsList =
        continuousVerificationService.getAllCVExecutionsForTime(
            "badAccount", before, after, true, PageRequestBuilder.aPageRequest().build());
    assertTrue("There's  no cv logs execution", cvLogsList.size() == 0);
  }
}
