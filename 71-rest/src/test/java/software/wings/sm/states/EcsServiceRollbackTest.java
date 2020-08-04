package software.wings.sm.states;

import static io.harness.rule.OwnerRule.ARVIND;
import static io.harness.rule.OwnerRule.TMACARI;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;
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

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import software.wings.WingsBaseTest;
import software.wings.api.ContainerRollbackRequestElement;
import software.wings.api.ContainerServiceElement;
import software.wings.beans.Activity;
import software.wings.beans.AwsConfig;
import software.wings.beans.Service;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.states.utils.StateTimeoutUtils;

@RunWith(PowerMockRunner.class)
@PrepareForTest({StateTimeoutUtils.class})
@PowerMockIgnore({"javax.security.*", "javax.net.*"})
public class EcsServiceRollbackTest extends WingsBaseTest {
  @Mock private EcsStateHelper mockEcsStateHelper;

  @InjectMocks private final EcsServiceRollback ecsServiceRollback = new EcsServiceRollback("stateName");

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
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testGetTimeoutMillis() {
    PowerMockito.mockStatic(StateTimeoutUtils.class);
    when(StateTimeoutUtils.getEcsStateTimeoutFromContext(any())).thenReturn(10);
    assertThat(ecsServiceRollback.getTimeoutMillis(mock(ExecutionContextImpl.class))).isEqualTo(10);
  }
}
