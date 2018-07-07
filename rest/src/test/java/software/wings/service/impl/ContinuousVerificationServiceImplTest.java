package software.wings.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.beans.User;
import software.wings.dl.WingsPersistence;
import software.wings.security.AppPermissionSummary;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.UserPermissionInfo;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData;
import software.wings.service.impl.analysis.ContinuousVerificationServiceImpl;
import software.wings.service.intfc.AuthService;
import software.wings.sm.StateType;

import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Praveen on 5/31/2018
 */
public class ContinuousVerificationServiceImplTest extends WingsBaseTest {
  private String accountId;
  private String appId;
  private String stateExecutionId;
  private String workflowId;
  private String workflowExecutionId;
  private String serviceId;
  private String envId;
  private User user;

  @Mock private AuthService mockAuthService;
  @Mock private WingsPersistence mockWingsPersistence;
  @Mock private FieldEnd mockField;
  @Mock private Query<ContinuousVerificationExecutionMetaData> mockQuery;
  @Mock private UserPermissionInfo mockUserPermissionInfo;
  @InjectMocks private ContinuousVerificationServiceImpl cvService;

  private void setupMocks() {
    accountId = UUID.randomUUID().toString();
    appId = UUID.randomUUID().toString();
    stateExecutionId = UUID.randomUUID().toString();
    workflowId = UUID.randomUUID().toString();
    workflowExecutionId = UUID.randomUUID().toString();
    serviceId = UUID.randomUUID().toString();
    envId = UUID.randomUUID().toString();
    user = new User();

    MockitoAnnotations.initMocks(this);

    when(mockWingsPersistence.createQuery(ContinuousVerificationExecutionMetaData.class)).thenReturn(mockQuery);
    when(mockQuery.filter(anyString(), anyObject())).thenReturn(mockQuery);
    when(mockQuery.order(anyString())).thenReturn(mockQuery);
    when(mockQuery.field(anyString())).thenReturn(mockField);
    when(mockField.greaterThanOrEq(anyObject())).thenReturn(mockQuery);
    when(mockField.lessThan(anyObject())).thenReturn(mockQuery);
    when(mockField.in(anyObject())).thenReturn(mockQuery);
    when(mockQuery.asList()).thenReturn(Arrays.asList(getExecutionMetadata()));

    when(mockUserPermissionInfo.getAppPermissionMapInternal()).thenReturn(new HashMap<String, AppPermissionSummary>() {
      { put(appId, buildAppPermissionSummary()); }
    });

    when(mockAuthService.getUserPermissionInfo(accountId, user)).thenReturn(mockUserPermissionInfo);
  }

  private ContinuousVerificationExecutionMetaData getExecutionMetadata() {
    return ContinuousVerificationExecutionMetaData.builder()
        .accountId(accountId)
        .applicationId(appId)
        .appName("dummy")
        .artifactName("cv dummy artifact")
        .envName("cv dummy env")
        .envId(envId)
        .phaseName("dummy phase")
        .pipelineName("dummy pipeline")
        .workflowName("dummy workflow")
        .pipelineStartTs(1519200000000L)
        .workflowStartTs(1519200000000L)
        .serviceId(serviceId)
        .serviceName("dummy service")
        .stateType(StateType.APM_VERIFICATION)
        .workflowId(workflowId)
        .workflowExecutionId(workflowExecutionId)
        .build();
  }

  private AppPermissionSummary buildAppPermissionSummary() {
    Map<Action, Set<String>> servicePermissions = new HashMap<Action, Set<String>>() {
      { put(Action.READ, Sets.newHashSet(serviceId)); }
    };
    Map<Action, Set<String>> envPermissions = new HashMap<Action, Set<String>>() {
      { put(Action.READ, Sets.newHashSet(envId)); }
    };
    Map<Action, Set<String>> pipelinePermissions = new HashMap<Action, Set<String>>() {
      { put(Action.READ, Sets.newHashSet()); }
    };
    Map<Action, Set<String>> workflowPermissions = new HashMap<Action, Set<String>>() {
      { put(Action.READ, Sets.newHashSet(workflowId)); }
    };

    return AppPermissionSummary.builder()
        .envPermissions(null)
        .servicePermissions(servicePermissions)
        .envPermissions(envPermissions)
        .workflowPermissions(workflowPermissions)
        .pipelinePermissions(pipelinePermissions)
        .build();
  }
  @Test
  public void testNullUser() throws ParseException {
    setupMocks();
    LinkedHashMap<Long,
        LinkedHashMap<String,
            LinkedHashMap<String,
                LinkedHashMap<String, LinkedHashMap<String, List<ContinuousVerificationExecutionMetaData>>>>>>
        execData = cvService.getCVExecutionMetaData(accountId, 1519200000000L, 1519200000001L, null);

    assertNotNull("Execution data is not null", execData);
    assertEquals("Execution data should be empty", 0, execData.size());
  }

  @Test
  public void testAllValidPermissions() throws ParseException {
    setupMocks();
    LinkedHashMap<Long,
        LinkedHashMap<String,
            LinkedHashMap<String,
                LinkedHashMap<String, LinkedHashMap<String, List<ContinuousVerificationExecutionMetaData>>>>>>
        execData = cvService.getCVExecutionMetaData(accountId, 1519200000000L, 1519200000001L, user);

    assertNotNull("Execution data is not null", execData);
    assertEquals("Execution data should be empty", 1, execData.size());
  }

  @Test
  public void testNoPermissionsForEnvironment() throws ParseException {
    setupMocks();
    AppPermissionSummary permissionSummary = buildAppPermissionSummary();
    permissionSummary.setEnvPermissions(new HashMap<>());
    when(mockUserPermissionInfo.getAppPermissionMapInternal()).thenReturn(new HashMap<String, AppPermissionSummary>() {
      { put(appId, permissionSummary); }
    });

    LinkedHashMap<Long,
        LinkedHashMap<String,
            LinkedHashMap<String,
                LinkedHashMap<String, LinkedHashMap<String, List<ContinuousVerificationExecutionMetaData>>>>>>
        execData = cvService.getCVExecutionMetaData(accountId, 1519200000000L, 1519200000001L, user);

    assertNotNull("Execution data is not null", execData);
    assertEquals("Execution data should be empty", 0, execData.size());
  }

  @Test
  public void testNoPermissionsForService() throws ParseException {
    setupMocks();
    AppPermissionSummary permissionSummary = buildAppPermissionSummary();
    permissionSummary.setServicePermissions(new HashMap<>());
    when(mockUserPermissionInfo.getAppPermissionMapInternal()).thenReturn(new HashMap<String, AppPermissionSummary>() {
      { put(appId, permissionSummary); }
    });
    LinkedHashMap<Long,
        LinkedHashMap<String,
            LinkedHashMap<String,
                LinkedHashMap<String, LinkedHashMap<String, List<ContinuousVerificationExecutionMetaData>>>>>>
        execData = cvService.getCVExecutionMetaData(accountId, 1519200000000L, 1519200000001L, user);

    assertNotNull("Execution data is not null", execData);
    assertEquals("Execution data should be empty", 0, execData.size());
  }
}
