package software.wings.sm.states;

import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder.aWorkflowExecution;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.version.VersionInfoManager;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.app.MainConfiguration;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.User;
import software.wings.beans.artifact.Artifact;
import software.wings.common.Constants;
import software.wings.dl.WingsPersistence;
import software.wings.security.AppPermissionSummary;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.UserPermissionInfo;
import software.wings.service.impl.analysis.ContinuousVerificationService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowExecutionBaselineService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Praveen on 5/31/2018
 */
public class APMStateVerificationTestBase extends WingsBaseTest {
  protected String accountId;
  protected String appId;
  protected String stateExecutionId;
  protected String workflowId;
  protected String workflowExecutionId;
  protected String serviceId;
  protected User user;

  @Mock protected ExecutionContextImpl executionContext;
  @Mock protected AuthService mockAuthService;
  @Mock protected BroadcasterFactory broadcasterFactory;
  @Mock protected WorkflowStandardParams workflowStandardParams;

  @Inject protected WingsPersistence wingsPersistence;
  @Inject protected AppService appService;
  @Inject protected SettingsService settingsService;
  @Inject protected WaitNotifyEngine waitNotifyEngine;
  @Inject protected DelegateService delegateService;
  @Inject protected MainConfiguration configuration;
  @Inject protected SecretManager secretManager;
  @Inject protected ContinuousVerificationService continuousVerificationService;
  @Inject protected WorkflowExecutionBaselineService workflowExecutionBaselineService;
  @Inject protected FeatureFlagService featureFlagService;

  @Inject protected WorkflowExecutionService workflowExecutionService;

  @Mock protected PhaseElement phaseElement;
  @Mock protected Environment environment;
  @Mock protected Application application;
  @Mock protected Artifact artifact;
  @Mock protected StateExecutionInstance stateExecutionInstance;
  @Mock private UserPermissionInfo mockUserPermissionInfo;
  @Inject protected VersionInfoManager versionInfoManager;

  protected void setupCommon() {
    accountId = UUID.randomUUID().toString();
    appId = UUID.randomUUID().toString();
    stateExecutionId = UUID.randomUUID().toString();
    workflowId = UUID.randomUUID().toString();
    workflowExecutionId = UUID.randomUUID().toString();
    serviceId = UUID.randomUUID().toString();
    user = new User();

    wingsPersistence.save(Application.Builder.anApplication().withUuid(appId).withAccountId(accountId).build());
    wingsPersistence.save(aWorkflowExecution()
                              .withAppId(appId)
                              .withWorkflowId(workflowId)
                              .withUuid(workflowExecutionId)
                              .withStartTs(1519200000000L)
                              .withName("dummy workflow")
                              .build());
    configuration.getPortal().setJwtExternalServiceSecret(accountId);
  }

  protected void setupCommonMocks() {
    when(executionContext.getAppId()).thenReturn(appId);
    when(executionContext.getWorkflowExecutionId()).thenReturn(workflowExecutionId);
    when(executionContext.getStateExecutionInstanceId()).thenReturn(stateExecutionId);
    when(executionContext.getWorkflowExecutionName()).thenReturn("dummy workflow");
    when(executionContext.renderExpression(anyString())).then(returnsFirstArg());
    when(executionContext.getWorkflowId()).thenReturn(workflowId);

    when(phaseElement.getServiceElement())
        .thenReturn(ServiceElement.Builder.aServiceElement().withName("dummy").withUuid("1").build());
    when(executionContext.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM)).thenReturn(phaseElement);
    when(environment.getName()).thenReturn("dummy env");
    when(executionContext.getEnv()).thenReturn(environment);
    when(application.getName()).thenReturn("dummuy app");
    when(executionContext.getApp()).thenReturn(application);
    when(artifact.getDisplayName()).thenReturn("dummy artifact");
    when(executionContext.getArtifactForService(anyString())).thenReturn(artifact);
    when(stateExecutionInstance.getStartTs()).thenReturn(1519200000000L);
    when(executionContext.getStateExecutionInstance()).thenReturn(stateExecutionInstance);

    Broadcaster broadcaster = mock(Broadcaster.class);
    when(broadcaster.broadcast(anyObject())).thenReturn(null);
    when(broadcasterFactory.lookup(anyObject(), anyBoolean())).thenReturn(broadcaster);
    setInternalState(delegateService, "broadcasterFactory", broadcasterFactory);
    setInternalState(continuousVerificationService, "authService", mockAuthService);
    // Setup authService for continuousVerificationService
    when(mockUserPermissionInfo.getAppPermissionMapInternal()).thenReturn(new HashMap<String, AppPermissionSummary>() {
      { put(appId, buildAppPermissionSummary()); }
    });

    when(mockAuthService.getUserPermissionInfo(accountId, user)).thenReturn(mockUserPermissionInfo);
  }

  private AppPermissionSummary buildAppPermissionSummary() {
    Map<Action, Set<String>> servicePermissions = new HashMap<Action, Set<String>>() {
      { put(Action.READ, Sets.newHashSet(serviceId)); }
    };
    Map<Action, Set<String>> envPermissions = new HashMap<Action, Set<String>>() {
      { put(Action.READ, Sets.newHashSet()); }
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
        //        .envPermissions(envPermissions)
        .workflowPermissions(workflowPermissions)
        .pipelinePermissions(pipelinePermissions)
        .build();
  }
}
