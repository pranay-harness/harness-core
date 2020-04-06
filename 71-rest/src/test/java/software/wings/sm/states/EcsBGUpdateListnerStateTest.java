package software.wings.sm.states;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.SATYAM;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static software.wings.api.DeploymentType.ECS;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.EcsInfrastructureMapping.Builder.anEcsInfrastructureMapping;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.CLUSTER_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import com.google.common.collect.ImmutableMap;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.ContainerServiceElement;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.api.ecs.EcsBGSetupData;
import software.wings.api.ecs.EcsListenerUpdateStateExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.SettingAttribute;
import software.wings.helpers.ext.ecs.request.EcsListenerUpdateRequestConfigData;
import software.wings.helpers.ext.ecs.response.EcsCommandExecutionResponse;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.WorkflowStandardParams;

public class EcsBGUpdateListnerStateTest extends WingsBaseTest {
  @Mock private AppService mockAppService;
  @Mock private InfrastructureMappingService mockInfrastructureMappingService;
  @Mock private DelegateService mockDelegateService;
  @Mock private SecretManager mockSecretManager;
  @Mock private SettingsService mockSettingsService;
  @Mock private ActivityService mockActivityService;
  @Mock private EcsStateHelper mockEcsStateHelper;
  @Mock private LogService mockLogService;

  @InjectMocks private EcsBGUpdateListnerState state = new EcsBGUpdateListnerState("stateName");
  @InjectMocks
  private EcsBGUpdateListnerRollbackState rollbackState = new EcsBGUpdateListnerRollbackState("rollbackState");

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testExecute() {
    state.setDownsizeOldService(true);
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    PhaseElement phaseElement = PhaseElement.builder()
                                    .deploymentType("ECS")
                                    .serviceElement(ServiceElement.builder().uuid(SERVICE_ID).build())
                                    .build();
    doReturn(phaseElement).when(mockContext).getContextElement(any(), anyString());
    WorkflowStandardParams mockParams = mock(WorkflowStandardParams.class);
    doReturn(mockParams).when(mockContext).getContextElement(any());
    Environment environment = anEnvironment().uuid(ENV_ID).name(ENV_NAME).build();
    doReturn(environment).when(mockParams).getEnv();
    Application application = anApplication().uuid(APP_ID).name(APP_NAME).accountId(ACCOUNT_ID).build();
    doReturn(application).when(mockAppService).get(anyString());
    EcsInfrastructureMapping mapping = anEcsInfrastructureMapping()
                                           .withUuid(INFRA_MAPPING_ID)
                                           .withClusterName(CLUSTER_NAME)
                                           .withRegion("us-east-1")
                                           .withVpcId("vpc-id")
                                           .withAssignPublicIp(true)
                                           .withLaunchType("Ec2")
                                           .build();
    doReturn(mapping).when(mockInfrastructureMappingService).get(anyString(), anyString());
    doReturn(INFRA_MAPPING_ID).when(mockContext).fetchInfraMappingId();
    doReturn(singletonList(ContainerServiceElement.builder()
                               .infraMappingId(INFRA_MAPPING_ID)
                               .deploymentType(ECS)
                               .clusterName(CLUSTER_NAME)
                               .newEcsServiceName("EcsSvc__2")
                               .ecsRegion("us-east-1")
                               .targetGroupForNewService("TgtNew")
                               .targetGroupForExistingService("TgtOld")
                               .ecsBGSetupData(EcsBGSetupData.builder()
                                                   .prodEcsListener("ProdLArn")
                                                   .stageEcsListener("StageLArn")
                                                   .downsizedServiceName("EcsSvc__1")
                                                   .build())
                               .build()))
        .when(mockContext)
        .getContextElementList(any());
    Activity activity = Activity.builder().uuid(ACTIVITY_ID).build();
    doReturn(activity).when(mockEcsStateHelper).createActivity(any(), anyString(), anyString(), any(), any());
    SettingAttribute cloudProvider = aSettingAttribute().withValue(AwsConfig.builder().build()).build();
    doReturn(cloudProvider).when(mockSettingsService).get(anyString());
    doReturn(emptyList()).when(mockSecretManager).getEncryptionDetails(any(), anyString(), anyString());
    ExecutionResponse response = state.execute(mockContext);
    ArgumentCaptor<EcsListenerUpdateRequestConfigData> captor =
        ArgumentCaptor.forClass(EcsListenerUpdateRequestConfigData.class);
    verify(mockEcsStateHelper)
        .queueDelegateTaskForEcsListenerUpdate(
            any(), any(), any(), any(), anyString(), anyString(), anyString(), captor.capture(), anyList());
    EcsListenerUpdateRequestConfigData config = captor.getValue();
    assertThat(config).isNotNull();
    assertThat(config.getProdListenerArn()).isEqualTo("ProdLArn");
    assertThat(config.getStageListenerArn()).isEqualTo("StageLArn");
    assertThat(config.getServiceName()).isEqualTo("EcsSvc__2");
    assertThat(config.getClusterName()).isEqualTo(CLUSTER_NAME);
    assertThat(config.getRegion()).isEqualTo("us-east-1");
    assertThat(config.getServiceNameDownsized()).isEqualTo("EcsSvc__1");
    assertThat(config.isDownsizeOldService()).isEqualTo(true);
    assertThat(config.getTargetGroupForNewService()).isEqualTo("TgtNew");
    assertThat(config.getTargetGroupForExistingService()).isEqualTo("TgtOld");

    doReturn(singletonList(
                 ContainerServiceElement.builder().deploymentType(ECS).infraMappingId(INFRA_MAPPING_ID + "1").build()))
        .when(mockContext)
        .getContextElementList(any());
    response = rollbackState.execute(mockContext);
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.SKIPPED);
    assertThat(response.getStateExecutionData().getErrorMsg()).isEqualTo("No context found for rollback. Skipping.");
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testHandleAsyncResponse() {
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    EcsCommandExecutionResponse delegateResponse =
        EcsCommandExecutionResponse.builder().commandExecutionStatus(SUCCESS).build();
    EcsListenerUpdateStateExecutionData data = EcsListenerUpdateStateExecutionData.builder().build();
    doReturn(data).when(mockContext).getStateExecutionData();
    state.handleAsyncResponse(mockContext, ImmutableMap.of(ACTIVITY_ID, delegateResponse));
    assertThat(data.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }
}