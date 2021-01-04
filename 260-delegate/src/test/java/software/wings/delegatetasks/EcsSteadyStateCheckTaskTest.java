package software.wings.delegatetasks;

import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.SATYAM;

import static software.wings.utils.WingsTestConstants.DELEGATE_ID;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.container.ContainerInfo;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.TaskData;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.container.EcsSteadyStateCheckParams;
import software.wings.beans.container.EcsSteadyStateCheckResponse;
import software.wings.cloudprovider.aws.EcsContainerService;
import software.wings.service.impl.AwsHelperService;

import com.amazonaws.services.ecs.model.DescribeServicesResult;
import com.amazonaws.services.ecs.model.Service;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@TargetModule(Module._930_DELEGATE_TASKS)
public class EcsSteadyStateCheckTaskTest extends WingsBaseTest {
  @Mock private AwsHelperService mockAwsHelperService;
  @Mock private DelegateLogService mockDelegateLogService;
  @Mock private EcsContainerService mockEcsContainerService;

  @InjectMocks
  private EcsSteadyStateCheckTask task =
      new EcsSteadyStateCheckTask(DelegateTaskPackage.builder()
                                      .delegateId(DELEGATE_ID)
                                      .data(TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build())
                                      .build(),
          null, notifyResponseData -> {}, () -> true);
  @Before
  public void setUp() throws Exception {
    on(task).set("awsHelperService", mockAwsHelperService);
    on(task).set("delegateLogService", mockDelegateLogService);
    on(task).set("ecsContainerService", mockEcsContainerService);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testRun() {
    doReturn(new DescribeServicesResult().withServices(
                 new Service().withServiceName("Name").withClusterArn("Cluster").withDesiredCount(1)))
        .when(mockAwsHelperService)
        .describeServices(anyString(), any(), any(), any());
    doNothing().when(mockEcsContainerService).waitForTasksToBeInRunningStateButDontThrowException(any());
    doNothing().when(mockEcsContainerService).waitForServiceToReachSteadyState(eq(0), any());
    doReturn(singletonList(ContainerInfo.builder().containerId("cid").hostName("host").newContainer(true).build()))
        .when(mockEcsContainerService)
        .getContainerInfosAfterEcsWait(anyString(), any(), anyList(), anyString(), anyString(), anyList(), any());
    EcsSteadyStateCheckResponse response = task.run(new Object[] {EcsSteadyStateCheckParams.builder().build()});
    assertThat(response).isNotNull();
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(response.getContainerInfoList()).isNotNull();
    assertThat(1).isEqualTo(response.getContainerInfoList().size());
    assertThat("host").isEqualTo(response.getContainerInfoList().get(0).getHostName());
    assertThat("cid").isEqualTo(response.getContainerInfoList().get(0).getContainerId());
  }
}
