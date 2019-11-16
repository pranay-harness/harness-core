package software.wings.sm.states;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.SATYAM;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;
import static software.wings.api.CommandStateExecutionData.Builder.aCommandStateExecutionData;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.EcsInfrastructureMapping.Builder.anEcsInfrastructureMapping;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.beans.command.EcsSetupParams.EcsSetupParamsBuilder.anEcsSetupParams;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.CLUSTER_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;

import com.google.common.collect.ImmutableMap;

import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import software.wings.WingsBaseTest;
import software.wings.api.CommandStateExecutionData;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.beans.Activity;
import software.wings.beans.AwsConfig;
import software.wings.beans.Service;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.command.ContainerSetupCommandUnitExecutionData;
import software.wings.beans.command.EcsSetupParams;
import software.wings.beans.container.ImageDetails;
import software.wings.helpers.ext.ecs.request.EcsServiceSetupRequest;
import software.wings.helpers.ext.ecs.response.EcsCommandExecutionResponse;
import software.wings.helpers.ext.ecs.response.EcsServiceSetupResponse;
import software.wings.service.impl.artifact.ArtifactCollectionUtils;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;

public class EcsDaemonServiceSetupTest extends WingsBaseTest {
  @Mock private SecretManager mockSecretManager;
  @Mock private EcsStateHelper mockEcsStateHelper;
  @Mock private ActivityService mockActivityService;
  @Mock private SettingsService mockSettingsService;
  @Mock private DelegateService mockDelegateService;
  @Mock private ArtifactCollectionUtils mockArtifactCollectionUtils;
  @Mock private ServiceResourceService mockServiceResourceService;
  @Mock private InfrastructureMappingService mockInfrastructureMappingService;

  @InjectMocks private EcsDaemonServiceSetup state = new EcsDaemonServiceSetup("stateName");

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testExecute() {
    state.setEcsServiceName("EcsSvc");
    state.setRoleArn("RoleArn");
    state.setLoadBalancerName("LbName");
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    when(mockContext.renderExpression(anyString())).thenAnswer(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        return (String) args[0];
      }
    });
    EcsSetUpDataBag bag = EcsSetUpDataBag.builder()
                              .service(Service.builder().uuid(SERVICE_ID).name(SERVICE_NAME).build())
                              .application(anApplication().uuid(APP_ID).name(APP_NAME).build())
                              .environment(anEnvironment().uuid(ENV_ID).name(ENV_NAME).build())
                              .ecsInfrastructureMapping(anEcsInfrastructureMapping()
                                                            .withUuid(INFRA_MAPPING_ID)
                                                            .withClusterName(CLUSTER_NAME)
                                                            .withRegion("us-east-1")
                                                            .withVpcId("vpc-id")
                                                            .withAssignPublicIp(true)
                                                            .withLaunchType("Ec2")
                                                            .build())
                              .awsConfig(AwsConfig.builder().build())
                              .encryptedDataDetails(emptyList())
                              .build();
    doReturn(bag).when(mockEcsStateHelper).prepareBagForEcsSetUp(any(), anyInt(), any(), any(), any(), any(), any());
    Activity activity = Activity.builder().uuid(ACTIVITY_ID).build();
    doReturn(activity).when(mockEcsStateHelper).createActivity(any(), anyString(), anyString(), any(), any());
    EcsSetupParams params =
        anEcsSetupParams().withBlueGreen(false).withServiceName("EcsSvc").withClusterName(CLUSTER_NAME).build();
    doReturn(params).when(mockEcsStateHelper).buildContainerSetupParams(any(), any());
    CommandStateExecutionData executionData = aCommandStateExecutionData().build();
    doReturn(executionData).when(mockEcsStateHelper).getStateExecutionData(any(), anyString(), any(), any());
    EcsSetupContextVariableHolder holder = EcsSetupContextVariableHolder.builder().build();
    doReturn(holder).when(mockEcsStateHelper).renderEcsSetupContextVariables(any());
    doReturn("DEL_TASK_ID")
        .when(mockEcsStateHelper)
        .createAndQueueDelegateTaskForEcsServiceSetUp(any(), any(), any(), any());
    ExecutionResponse response = state.execute(mockContext);
    ArgumentCaptor<EcsSetupStateConfig> captor = ArgumentCaptor.forClass(EcsSetupStateConfig.class);
    verify(mockEcsStateHelper).buildContainerSetupParams(any(), captor.capture());
    EcsSetupStateConfig config = captor.getValue();
    assertThat(config).isNotNull();
    assertThat(config.getServiceName()).isEqualTo(SERVICE_NAME);
    assertThat(config.getApp()).isNotNull();
    assertThat(config.getApp().getUuid()).isEqualTo(APP_ID);
    assertThat(config.getService()).isNotNull();
    assertThat(config.getService().getUuid()).isEqualTo(SERVICE_ID);
    assertThat(config.getClusterName()).isEqualTo(CLUSTER_NAME);
    assertThat(config.getEcsServiceName()).isEqualTo("EcsSvc");
    assertThat(config.getLoadBalancerName()).isEqualTo("LbName");
    assertThat(config.getRoleArn()).isEqualTo("RoleArn");
    assertThat(config.isDaemonSchedulingStrategy()).isEqualTo(true);
    ArgumentCaptor<EcsServiceSetupRequest> captor2 = ArgumentCaptor.forClass(EcsServiceSetupRequest.class);
    verify(mockEcsStateHelper).createAndQueueDelegateTaskForEcsServiceSetUp(captor2.capture(), any(), any(), any());
    EcsServiceSetupRequest request = captor2.getValue();
    assertThat(request).isNotNull();
    assertThat(request.getEcsSetupParams()).isNotNull();
    assertThat(request.getEcsSetupParams().getClusterName()).isEqualTo(CLUSTER_NAME);
    assertThat(request.getCluster()).isEqualTo(CLUSTER_NAME);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testHandleAsyncResponse() {
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    EcsCommandExecutionResponse delegateResponse =
        EcsCommandExecutionResponse.builder()
            .commandExecutionStatus(SUCCESS)
            .ecsCommandResponse(EcsServiceSetupResponse.builder()
                                    .isBlueGreen(false)
                                    .setupData(ContainerSetupCommandUnitExecutionData.builder()
                                                   .containerServiceName("ContainerServiceName")
                                                   .instanceCountForLatestVersion(2)
                                                   .build())
                                    .build())
            .build();
    CommandStateExecutionData executionData =
        aCommandStateExecutionData()
            .withContainerSetupParams(anEcsSetupParams().withInfraMappingId(INFRA_MAPPING_ID).build())
            .build();
    doReturn(executionData).when(mockContext).getStateExecutionData();
    PhaseElement phaseElement =
        PhaseElement.builder().serviceElement(ServiceElement.builder().uuid(SERVICE_ID).build()).build();
    doReturn(phaseElement).when(mockContext).getContextElement(any(), anyString());
    Artifact artifact = anArtifact().withRevision("rev").build();
    doReturn(artifact).when(mockContext).getDefaultArtifactForService(anyString());
    ImageDetails details = ImageDetails.builder().name("imgName").tag("imgTag").build();
    doReturn(details).when(mockArtifactCollectionUtils).fetchContainerImageDetails(any(), anyString());
    ExecutionResponse response = state.handleAsyncResponse(mockContext, ImmutableMap.of(ACTIVITY_ID, delegateResponse));
    verify(mockEcsStateHelper).populateFromDelegateResponse(any(), any(), any());
  }
}