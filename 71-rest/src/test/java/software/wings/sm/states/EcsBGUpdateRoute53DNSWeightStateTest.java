package software.wings.sm.states;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.SATYAM;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
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

import io.harness.beans.DelegateTask;
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
import software.wings.api.ecs.EcsRoute53WeightUpdateStateExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.SettingAttribute;
import software.wings.helpers.ext.ecs.request.EcsBGRoute53DNSWeightUpdateRequest;
import software.wings.helpers.ext.ecs.response.EcsCommandExecutionResponse;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;

public class EcsBGUpdateRoute53DNSWeightStateTest extends WingsBaseTest {
  @Mock private AppService mockAppService;
  @Mock private SecretManager mockSecretManager;
  @Mock private SettingsService mockSettingsService;
  @Mock private ActivityService mockActivityService;
  @Mock private DelegateService mockDelegateService;
  @Mock private InfrastructureMappingService mockInfrastructureMappingService;

  @InjectMocks private EcsBGUpdateRoute53DNSWeightState state = new EcsBGUpdateRoute53DNSWeightState("stateName");

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testExecute() {
    state.setDownsizeOldService(true);
    state.setRecordTTL(60);
    state.setOldServiceDNSWeight(-1);
    state.setNewServiceDNSWeight(101);
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    PhaseElement phaseElement = PhaseElement.builder()
                                    .deploymentType("ECS")
                                    .serviceElement(ServiceElement.builder().uuid(SERVICE_ID).build())
                                    .build();
    doReturn(phaseElement).when(mockContext).getContextElement(any(), anyString());
    doReturn(singletonList(ContainerServiceElement.builder()
                               .infraMappingId(INFRA_MAPPING_ID)
                               .deploymentType(ECS)
                               .clusterName(CLUSTER_NAME)
                               .newEcsServiceName("EcsSvc__2")
                               .ecsRegion("us-east-1")
                               .targetGroupForNewService("TgtNew")
                               .targetGroupForExistingService("TgtOld")
                               .ecsBGSetupData(EcsBGSetupData.builder()
                                                   .parentRecordName("ParentName")
                                                   .parentRecordHostedZoneId("ParentId")
                                                   .oldServiceDiscoveryArn("OldSDSArn")
                                                   .newServiceDiscoveryArn("NewSDSArn")
                                                   .build())
                               .build()))
        .when(mockContext)
        .getContextElementList(any());
    doReturn(INFRA_MAPPING_ID).when(mockContext).fetchInfraMappingId();
    Environment environment = anEnvironment().uuid(ENV_ID).name(ENV_NAME).build();
    doReturn(environment).when(mockContext).getEnv();
    Application application = anApplication().uuid(APP_ID).name(APP_NAME).accountId(ACCOUNT_ID).build();
    doReturn(application).when(mockContext).getApp();
    Activity activity = Activity.builder().uuid(ACTIVITY_ID).build();
    doReturn(activity).when(mockActivityService).save(any());
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
    SettingAttribute cloudProvider = aSettingAttribute().withValue(AwsConfig.builder().build()).build();
    doReturn(cloudProvider).when(mockSettingsService).get(anyString());
    doReturn(emptyList()).when(mockSecretManager).getEncryptionDetails(any(), anyString(), anyString());
    ExecutionResponse response = state.execute(mockContext);
    ArgumentCaptor<DelegateTask> captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(mockDelegateService).queueTask(captor.capture());
    DelegateTask delegateTask = captor.getValue();
    assertThat(delegateTask).isNotNull();
    assertThat(delegateTask.getData().getParameters()).isNotNull();
    assertThat(2).isEqualTo(delegateTask.getData().getParameters().length);
    assertThat(delegateTask.getData().getParameters()[0] instanceof EcsBGRoute53DNSWeightUpdateRequest).isTrue();
    EcsBGRoute53DNSWeightUpdateRequest params =
        (EcsBGRoute53DNSWeightUpdateRequest) delegateTask.getData().getParameters()[0];
    assertThat(params.getAppId()).isEqualTo(APP_ID);
    assertThat(params.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(params.getActivityId()).isEqualTo(ACTIVITY_ID);
    assertThat(params.isRollback()).isFalse();
    assertThat(params.getServiceName()).isEqualTo("EcsSvc__2");
    assertThat(params.isDownsizeOldService()).isTrue();
    assertThat(params.getOldServiceWeight()).isEqualTo(0);
    assertThat(params.getNewServiceWeight()).isEqualTo(100);
    assertThat(params.getParentRecordName()).isEqualTo("ParentName");
    assertThat(params.getParentRecordHostedZoneId()).isEqualTo("ParentId");
    assertThat(params.getOldServiceDiscoveryArn()).isEqualTo("OldSDSArn");
    assertThat(params.getNewServiceDiscoveryArn()).isEqualTo("NewSDSArn");
    assertThat(params.getTtl()).isEqualTo(60);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testHandleAsyncResponse() {
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    EcsCommandExecutionResponse delegateResponse =
        EcsCommandExecutionResponse.builder().commandExecutionStatus(SUCCESS).build();
    EcsRoute53WeightUpdateStateExecutionData data = EcsRoute53WeightUpdateStateExecutionData.builder().build();
    doReturn(data).when(mockContext).getStateExecutionData();
    state.handleAsyncResponse(mockContext, ImmutableMap.of(ACTIVITY_ID, delegateResponse));
    verify(mockActivityService).updateStatus(anyString(), anyString(), eq(ExecutionStatus.SUCCESS));
  }
}