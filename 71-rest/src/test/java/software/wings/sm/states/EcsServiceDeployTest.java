package software.wings.sm.states;

import static io.harness.rule.OwnerRule.SATYAM;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.EcsInfrastructureMapping.Builder.anEcsInfrastructureMapping;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.InstanceUnitType.PERCENTAGE;
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
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import software.wings.WingsBaseTest;
import software.wings.api.ContainerServiceElement;
import software.wings.beans.Activity;
import software.wings.beans.AwsConfig;
import software.wings.beans.Service;
import software.wings.helpers.ext.container.ContainerDeploymentManagerHelper;
import software.wings.helpers.ext.ecs.request.EcsServiceDeployRequest;
import software.wings.helpers.ext.ecs.response.EcsCommandExecutionResponse;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;

public class EcsServiceDeployTest extends WingsBaseTest {
  @Mock private SecretManager mockSecretManager;
  @Mock private EcsStateHelper mockEcsStateHelper;
  @Mock private SettingsService mockSettingsService;
  @Mock private DelegateService mockDelegateService;
  @Mock private ActivityService mockActivityService;
  @Mock private ServiceResourceService mockServiceResourceService;
  @Mock private ServiceTemplateService mockServiceTemplateService;
  @Mock private InfrastructureMappingService mockInfrastructureMappingService;
  @Mock private ContainerDeploymentManagerHelper mockContainerDeploymentHelper;

  @InjectMocks private EcsServiceDeploy state = new EcsServiceDeploy("stateName");

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testExecute() {
    state.setInstanceCount("20");
    state.setInstanceUnitType(PERCENTAGE);
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    when(mockContext.renderExpression(anyString())).thenAnswer(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        return (String) args[0];
      }
    });
    EcsDeployDataBag bag =
        EcsDeployDataBag.builder()
            .service(Service.builder().uuid(SERVICE_ID).name(SERVICE_NAME).build())
            .app(anApplication().uuid(APP_ID).name(APP_NAME).build())
            .env(anEnvironment().uuid(ENV_ID).name(ENV_NAME).build())
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
            .containerElement(ContainerServiceElement.builder().serviceSteadyStateTimeout(10).build())
            .build();
    doReturn(bag).when(mockEcsStateHelper).prepareBagForEcsDeploy(any(), any(), any(), any(), any(), anyBoolean());
    Activity activity = Activity.builder().uuid(ACTIVITY_ID).build();
    doReturn(activity).when(mockEcsStateHelper).createActivity(any(), anyString(), anyString(), any(), any());
    ExecutionResponse response = state.execute(mockContext);
    ArgumentCaptor<EcsServiceDeployRequest> captor = ArgumentCaptor.forClass(EcsServiceDeployRequest.class);
    verify(mockEcsStateHelper).createAndQueueDelegateTaskForEcsServiceDeploy(any(), captor.capture(), any(), any());
    EcsServiceDeployRequest request = captor.getValue();
    assertThat(request).isNotNull();
    assertThat(request.getEcsResizeParams()).isNotNull();
    assertThat(request.getEcsResizeParams().getInstanceCount()).isEqualTo(20);
    assertThat(request.getEcsResizeParams().getDownsizeInstanceUnitType()).isEqualTo(PERCENTAGE);
    assertThat(request.getActivityId()).isEqualTo(ACTIVITY_ID);
    assertThat(request.getCluster()).isEqualTo(CLUSTER_NAME);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testHandleAsyncResponse() {
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    EcsCommandExecutionResponse executionResponse = EcsCommandExecutionResponse.builder().build();
    state.handleAsyncResponse(mockContext, ImmutableMap.of(ACTIVITY_ID, executionResponse));
    verify(mockEcsStateHelper).handleDelegateResponseForEcsDeploy(any(), anyMap(), anyBoolean(), any(), any(), any());
  }
}