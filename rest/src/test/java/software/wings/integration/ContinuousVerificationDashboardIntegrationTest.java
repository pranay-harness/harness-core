package software.wings.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import software.wings.beans.RestResponse;
import software.wings.beans.User;
import software.wings.security.AppPermissionSummary;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.UserThreadLocal;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData;
import software.wings.service.impl.analysis.ContinuousVerificationService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.UserService;
import software.wings.sm.StateType;
import software.wings.utils.WingsIntegrationTestConstants;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

public class ContinuousVerificationDashboardIntegrationTest extends BaseIntegrationTest {
  @Inject ContinuousVerificationService continuousVerificationService;
  @Inject UserService userService;
  @Inject AuthService authService;
  @Inject AppService appService;

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
  }

  @Test
  public void getRecords() throws Exception {
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
    long before = now - 1, after = now + 5;
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
    ContinuousVerificationExecutionMetaData continuousVerificationExecutionMetaData1 =
        map.get(start)
            .get("cv dummy artifact")
            .get("cv dummy env/dummy workflow")
            .values()
            .iterator()
            .next()
            .get("dummy phase")
            .get(0);
    assertEquals(continuousVerificationExecutionMetaData1.getAccountId(), accountId);
    assertEquals(continuousVerificationExecutionMetaData1.getArtifactName(), "cv dummy artifact");
  }
}
