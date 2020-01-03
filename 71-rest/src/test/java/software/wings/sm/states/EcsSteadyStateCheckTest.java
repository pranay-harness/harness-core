package software.wings.sm.states;

import static io.harness.context.ContextElementType.INSTANCE;
import static io.harness.context.ContextElementType.STANDARD;
import static io.harness.rule.OwnerRule.SATYAM;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.EcsInfrastructureMapping.Builder.anEcsInfrastructureMapping;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.sm.InstanceStatusSummary.InstanceStatusSummaryBuilder.anInstanceStatusSummary;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.CLUSTER_NAME;
import static software.wings.utils.WingsTestConstants.COMPUTE_PROVIDER_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;

import com.google.common.collect.ImmutableMap;

import com.amazonaws.regions.Regions;
import io.harness.beans.DelegateTask;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ResponseData;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.PhaseElement;
import software.wings.api.ScriptStateExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.SettingAttribute;
import software.wings.beans.container.EcsSteadyStateCheckParams;
import software.wings.beans.container.EcsSteadyStateCheckResponse;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.helpers.ext.container.ContainerDeploymentManagerHelper;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.WorkflowStandardParams;

import java.util.Map;

public class EcsSteadyStateCheckTest extends WingsBaseTest {
  @Mock private AppService mockAppService;
  @Mock private SecretManager mockSecretManager;
  @Mock private ActivityService mockActivityService;
  @Mock private SettingsService mockSettingsService;
  @Mock private DelegateService mockDelegateService;
  @Mock private InfrastructureMappingService mockInfrastructureMappingService;
  @Mock private ContainerDeploymentManagerHelper mockContainerDeploymentManagerHelper;

  @InjectMocks private EcsSteadyStateCheck check = new EcsSteadyStateCheck("stateName");

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testExecute() {
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    PhaseElement mockPhaseElement = mock(PhaseElement.class);
    WorkflowStandardParams mockParams = mock(WorkflowStandardParams.class);
    EmbeddedUser currentUser = EmbeddedUser.builder().name("test").email("test@harness.io").build();
    mockParams.setCurrentUser(currentUser);
    when(mockParams.getCurrentUser()).thenReturn(currentUser);
    doReturn(mockPhaseElement).when(mockContext).getContextElement(any(), anyString());
    doReturn(mockParams).when(mockContext).getContextElement(eq(STANDARD));
    doReturn(null).when(mockContext).getContextElement(eq(INSTANCE));
    Application app = anApplication().uuid(APP_ID).name(APP_NAME).accountId(ACCOUNT_ID).build();
    doReturn(app).when(mockAppService).get(anyString());
    doReturn(app).when(mockContext).getApp();
    Environment env = anEnvironment().appId(APP_ID).uuid(ENV_ID).name(ENV_NAME).build();
    doReturn(env).when(mockContext).getEnv();
    doReturn(env).when(mockParams).getEnv();
    ContainerInfrastructureMapping containerInfrastructureMapping =
        anEcsInfrastructureMapping()
            .withUuid(INFRA_MAPPING_ID)
            .withRegion(Regions.US_EAST_1.getName())
            .withClusterName(CLUSTER_NAME)
            .withComputeProviderSettingId(COMPUTE_PROVIDER_ID)
            .withDeploymentType("ECS")
            .build();
    doReturn(containerInfrastructureMapping).when(mockInfrastructureMappingService).get(anyString(), anyString());
    Activity activity = Activity.builder().build();
    activity.setUuid(ACTIVITY_ID);
    doReturn(activity).when(mockActivityService).save(any());
    SettingAttribute awsConfig = aSettingAttribute().withValue(AwsConfig.builder().build()).build();
    doReturn(awsConfig).when(mockSettingsService).get(anyString());
    ExecutionResponse response = check.execute(mockContext);
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    ArgumentCaptor<DelegateTask> captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(mockDelegateService).queueTask(captor.capture());
    DelegateTask delegateTask = captor.getValue();
    assertThat(delegateTask).isNotNull();
    assertThat(delegateTask.getData().getParameters()).isNotNull();
    assertThat(1).isEqualTo(delegateTask.getData().getParameters().length);
    assertThat(delegateTask.getData().getParameters()[0] instanceof EcsSteadyStateCheckParams).isTrue();
    EcsSteadyStateCheckParams params = (EcsSteadyStateCheckParams) delegateTask.getData().getParameters()[0];
    assertThat("Ecs Steady State Check").isEqualTo(params.getCommandName());
    assertThat(APP_ID).isEqualTo(params.getAppId());
    assertThat(ACCOUNT_ID).isEqualTo(params.getAccountId());
    assertThat(ACTIVITY_ID).isEqualTo(params.getActivityId());
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testHandleAsyncResponse() {
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    WorkflowStandardParams mockParams = mock(WorkflowStandardParams.class);
    doReturn(mockParams).when(mockContext).getContextElement(eq(STANDARD));
    doReturn(APP_ID).when(mockParams).getAppId();
    ScriptStateExecutionData mockData = mock(ScriptStateExecutionData.class);
    doReturn(mockData).when(mockContext).getStateExecutionData();
    Map<String, ResponseData> delegateResponse = ImmutableMap.of(ACTIVITY_ID,
        EcsSteadyStateCheckResponse.builder()
            .executionStatus(ExecutionStatus.SUCCESS)
            .containerInfoList(singletonList(ContainerInfo.builder().hostName("host").containerId("cid").build()))
            .build());
    doReturn(singletonList(anInstanceStatusSummary()
                               .withInstanceElement(anInstanceElement().hostName("host").displayName("disp").build())
                               .build()))
        .when(mockContainerDeploymentManagerHelper)
        .getInstanceStatusSummaries(any(), anyList());
    ExecutionResponse response = check.handleAsyncResponse(mockContext, delegateResponse);
    verify(mockActivityService).updateStatus(anyString(), anyString(), any());
    verify(mockData).setStatus(any());
  }
}