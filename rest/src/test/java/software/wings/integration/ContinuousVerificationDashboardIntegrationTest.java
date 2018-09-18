package software.wings.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

import com.google.inject.Inject;

import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import software.wings.beans.FeatureName;
import software.wings.beans.RestResponse;
import software.wings.beans.User;
import software.wings.security.AppPermissionSummary;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.UserThreadLocal;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData;
import software.wings.service.impl.analysis.ContinuousVerificationService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.UserService;
import software.wings.sm.StateType;
import software.wings.utils.WingsIntegrationTestConstants;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

  @Before
  public void setUp() throws Exception {
    loginAdminUser();
    User user = userService.getUserByEmail(WingsIntegrationTestConstants.adminUserEmail);
    UserThreadLocal.set(user);
    stateExecutionId = UUID.randomUUID().toString();

    workflowExecutionId = UUID.randomUUID().toString();

    Map<String, AppPermissionSummary> appsMap =
        authService.getUserPermissionInfo(accountId, user).getAppPermissionMapInternal();

    // we need to user the testApplication, it has workflows defined under it by Datagen.
    appId = appService.getAppByName(accountId, "Test Application").getUuid();
    serviceId = appsMap.get(appId).getServicePermissions().get(Action.READ).iterator().next();
    workflowId = appsMap.get(appId).getWorkflowPermissions().get(Action.READ).iterator().next();
    setInternalState(continuousVerificationService, "featureFlagService", featureFlagService);
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

    assertFalse(response.getResource().isEmpty());

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
