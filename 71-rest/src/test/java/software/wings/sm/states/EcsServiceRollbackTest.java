package software.wings.sm.states;

import static io.harness.rule.OwnerRule.ARVIND;
import static io.harness.rule.OwnerRule.SATYAM;
import static io.harness.rule.OwnerRule.TMACARI;

import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.EcsInfrastructureMapping.Builder.anEcsInfrastructureMapping;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.CLUSTER_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.api.ContainerRollbackRequestElement;
import software.wings.api.ContainerServiceElement;
import software.wings.beans.Activity;
import software.wings.beans.AwsConfig;
import software.wings.beans.Service;
import software.wings.beans.container.AwsAutoScalarConfig;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class EcsServiceRollbackTest extends WingsBaseTest {
  @Mock private EcsStateHelper mockEcsStateHelper;

  @InjectMocks private final EcsServiceRollback ecsServiceRollback = new EcsServiceRollback("stateName");

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testExecuteWithNullRollbackElement() {
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    doReturn(null).when(mockEcsStateHelper).getDeployElementFromSweepingOutput(any());
    ExecutionResponse response = ecsServiceRollback.execute(mockContext);
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.SKIPPED);
    assertThat(response.getStateExecutionData().getErrorMsg()).isEqualTo("No context found for rollback. Skipping.");
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testExecuteWithNullContainerElement() {
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    ContainerRollbackRequestElement deployElement = ContainerRollbackRequestElement.builder().build();
    doReturn(deployElement).when(mockEcsStateHelper).getDeployElementFromSweepingOutput(any());
    EcsDeployDataBag dataBag = EcsDeployDataBag.builder().build();
    doReturn(dataBag).when(mockEcsStateHelper).prepareBagForEcsDeploy(any(), any(), any(), any(), any(), anyBoolean());

    ExecutionResponse response = ecsServiceRollback.execute(mockContext);
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.SKIPPED);
    assertThat(response.getStateExecutionData().getErrorMsg()).isEqualTo("No container setup element found. Skipping.");
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testExecuteWithoutRollbackAllPhases() {
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    ContainerRollbackRequestElement deployElement = ContainerRollbackRequestElement.builder().build();
    doReturn(deployElement).when(mockEcsStateHelper).getDeployElementFromSweepingOutput(any());

    Activity activity = Activity.builder().uuid(ACTIVITY_ID).build();
    doReturn(activity).when(mockEcsStateHelper).createActivity(any(), any(), any(), any(), any());
    EcsDeployDataBag dataBag =
        EcsDeployDataBag.builder()
            .service(Service.builder().uuid(SERVICE_ID).name(SERVICE_NAME).build())
            .app(anApplication().uuid(APP_ID).name(APP_NAME).build())
            .env(anEnvironment().uuid(ENV_ID).name(ENV_NAME).build())
            .region("us-east-1")
            .ecsInfrastructureMapping(anEcsInfrastructureMapping()
                                          .withUuid(INFRA_MAPPING_ID)
                                          .withClusterName(CLUSTER_NAME)
                                          .withRegion("us-east-1")
                                          .withVpcId("vpc-id")
                                          .withAssignPublicIp(true)
                                          .withLaunchType("Ec2")
                                          .build())
            .rollbackElement(ContainerRollbackRequestElement.builder().build())
            .awsConfig(AwsConfig.builder().build())
            .encryptedDataDetails(emptyList())
            .containerElement(
                ContainerServiceElement.builder().clusterName(CLUSTER_NAME).serviceSteadyStateTimeout(10).build())
            .build();
    doReturn(dataBag).when(mockEcsStateHelper).prepareBagForEcsDeploy(any(), any(), any(), any(), any(), anyBoolean());
    doReturn("TASKID")
        .when(mockEcsStateHelper)
        .createAndQueueDelegateTaskForEcsServiceDeploy(any(), any(), any(), any());

    ExecutionResponse response = ecsServiceRollback.execute(mockContext);
    assertThat(ecsServiceRollback.isRollbackAllPhases()).isFalse();
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testHandleAsyncResponse() {
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    ecsServiceRollback.handleAsyncResponse(mockContext, null);
    verify(mockEcsStateHelper).handleDelegateResponseForEcsDeploy(any(), any(), anyBoolean(), any(), any(), any());
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testExecuteWithRollbackAllPhases() {
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    ContainerRollbackRequestElement deployElement = ContainerRollbackRequestElement.builder().build();
    doReturn(deployElement).when(mockEcsStateHelper).getDeployElementFromSweepingOutput(any());
    Activity activity = Activity.builder().uuid(ACTIVITY_ID).build();
    doReturn(activity).when(mockEcsStateHelper).createActivity(any(), any(), any(), any(), any());
    EcsDeployDataBag dataBag =
        EcsDeployDataBag.builder()
            .service(Service.builder().uuid(SERVICE_ID).name(SERVICE_NAME).build())
            .app(anApplication().uuid(APP_ID).name(APP_NAME).build())
            .env(anEnvironment().uuid(ENV_ID).name(ENV_NAME).build())
            .region("us-east-1")
            .ecsInfrastructureMapping(anEcsInfrastructureMapping()
                                          .withUuid(INFRA_MAPPING_ID)
                                          .withClusterName(CLUSTER_NAME)
                                          .withRegion("us-east-1")
                                          .withVpcId("vpc-id")
                                          .withAssignPublicIp(true)
                                          .withLaunchType("Ec2")
                                          .build())
            .rollbackElement(ContainerRollbackRequestElement.builder().build())
            .awsConfig(AwsConfig.builder().build())
            .encryptedDataDetails(emptyList())
            .containerElement(ContainerServiceElement.builder()
                                  .clusterName(CLUSTER_NAME)
                                  .previousAwsAutoScalarConfigs(singletonList(AwsAutoScalarConfig.builder().build()))
                                  .serviceSteadyStateTimeout(10)
                                  .build())
            .build();
    doReturn(dataBag).when(mockEcsStateHelper).prepareBagForEcsDeploy(any(), any(), any(), any(), any(), anyBoolean());
    doReturn("TASKID")
        .when(mockEcsStateHelper)
        .createAndQueueDelegateTaskForEcsServiceDeploy(any(), any(), any(), any());
    ExecutionResponse response = ecsServiceRollback.execute(mockContext);
    assertThat(ecsServiceRollback.isRollbackAllPhases()).isTrue();
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testGetTimeoutMillis() {
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    doReturn(10).when(mockEcsStateHelper).getEcsStateTimeoutFromContext(anyObject(), anyBoolean());
    assertThat(ecsServiceRollback.getTimeoutMillis(mockContext)).isEqualTo(10);
  }
}
