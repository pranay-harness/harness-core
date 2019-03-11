package software.wings.delegatetasks;

import static java.util.Collections.singletonList;
import static org.joor.Reflect.on;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static software.wings.beans.DelegateTask.DEFAULT_ASYNC_CALL_TIMEOUT;
import static software.wings.utils.WingsTestConstants.DELEGATE_ID;

import com.amazonaws.services.ecs.model.DescribeServicesResult;
import com.amazonaws.services.ecs.model.Service;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.TaskData;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.DelegateTask;
import software.wings.beans.TaskType;
import software.wings.beans.container.EcsSteadyStateCheckParams;
import software.wings.beans.container.EcsSteadyStateCheckResponse;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.cloudprovider.aws.EcsContainerService;
import software.wings.service.impl.AwsHelperService;

public class EcsSteadyStateCheckTaskTest extends WingsBaseTest {
  @Mock private AwsHelperService mockAwsHelperService;
  @Mock private DelegateLogService mockDelegateLogService;
  @Mock private EcsContainerService mockEcsContainerService;

  @InjectMocks
  private EcsSteadyStateCheckTask task =
      (EcsSteadyStateCheckTask) TaskType.ECS_STEADY_STATE_CHECK_TASK.getDelegateRunnableTask(DELEGATE_ID,
          DelegateTask.builder()
              .async(true)
              .data(TaskData.builder().timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build())
              .build(),
          notifyResponseData -> {}, () -> true);
  @Before
  public void setUp() throws Exception {
    on(task).set("awsHelperService", mockAwsHelperService);
    on(task).set("delegateLogService", mockDelegateLogService);
    on(task).set("ecsContainerService", mockEcsContainerService);
  }

  @Test
  public void testRun() {
    doReturn(new DescribeServicesResult().withServices(
                 new Service().withServiceName("Name").withClusterArn("Cluster").withDesiredCount(1)))
        .when(mockAwsHelperService)
        .describeServices(anyString(), any(), any(), any());
    doNothing().when(mockEcsContainerService).waitForTasksToBeInRunningStateButDontThrowException(any());
    doNothing().when(mockEcsContainerService).waitForServiceToReachSteadyState(eq(0), any());
    doReturn(singletonList(ContainerInfo.builder().containerId("cid").hostName("host").newContainer(true).build()))
        .when(mockEcsContainerService)
        .getContainerInfosAfterEcsWait(
            anyString(), any(), anyList(), anyString(), anyString(), anyList(), any(), eq(false));
    EcsSteadyStateCheckResponse response = task.run(new Object[] {EcsSteadyStateCheckParams.builder().build()});
    assertNotNull(response);
    assertEquals(ExecutionStatus.SUCCESS, response.getExecutionStatus());
    assertNotNull(response.getContainerInfoList());
    assertEquals(response.getContainerInfoList().size(), 1);
    assertEquals(response.getContainerInfoList().get(0).getHostName(), "host");
    assertEquals(response.getContainerInfoList().get(0).getContainerId(), "cid");
  }
}
