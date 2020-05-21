package io.harness.perpetualtask;

import static io.harness.rule.OwnerRule.HANTANG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.util.Durations;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.perpetualtask.grpc.PerpetualTaskServiceGrpcClient;
import io.harness.perpetualtask.k8s.watch.K8sWatchTaskParams;
import io.harness.rule.Owner;
import io.harness.serializer.KryoUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.beans.KubernetesClusterConfig;

import java.util.Map;

@Slf4j
public class PerpetualTaskWorkerTest extends CategoryTest {
  String accountId = "ACCOUNT_ID";
  String cloudProviderId = "CLOUD_PROVIDER_ID";

  String taskIdString1 = "TASK_ID_1";
  PerpetualTaskId taskId1 = PerpetualTaskId.newBuilder().setId(taskIdString1).build();
  PerpetualTaskAssignDetails task1 = PerpetualTaskAssignDetails.newBuilder().setTaskId(taskId1).build();

  String taskIdString2 = "TASK_ID_2";
  PerpetualTaskId taskId2 = PerpetualTaskId.newBuilder().setId(taskIdString2).build();
  PerpetualTaskAssignDetails task2 = PerpetualTaskAssignDetails.newBuilder().setTaskId(taskId2).build();

  KubernetesClusterConfig kubernetesClusterConfig;
  PerpetualTaskParams params;
  PerpetualTaskContext context;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock private PerpetualTaskServiceGrpcClient perpetualTaskServiceGrpcClient;
  @Mock private Map<String, PerpetualTaskExecutor> factoryMap;
  @Mock private TimeLimiter timeLimiter;
  @InjectMocks private PerpetualTaskWorker worker;

  @Before
  public void setUp() {
    kubernetesClusterConfig = KubernetesClusterConfig.builder().accountId(accountId).build();
    ByteString bytes = ByteString.copyFrom(KryoUtils.asBytes(kubernetesClusterConfig));
    K8sWatchTaskParams k8sWatchTaskParams =
        K8sWatchTaskParams.newBuilder().setCloudProviderId(cloudProviderId).setK8SClusterConfig(bytes).build();

    params = PerpetualTaskParams.newBuilder().setCustomizedParams(Any.pack(k8sWatchTaskParams)).build();

    PerpetualTaskSchedule schedule = PerpetualTaskSchedule.newBuilder().setInterval(Durations.fromSeconds(1)).build();
    context = PerpetualTaskContext.newBuilder().setTaskParams(params).setTaskSchedule(schedule).build();

    when(perpetualTaskServiceGrpcClient.perpetualTaskContext(isA(PerpetualTaskId.class))).thenReturn(context);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testUpdateAssignedTaskIds() {
    worker.updateAssignedTaskIds();
    verify(perpetualTaskServiceGrpcClient).perpetualTaskList(anyString());
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testStartTask() {
    worker.startTask(task1);
    worker.startTask(task2);
    assertThat(worker.getRunningTaskMap()).hasSize(2);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testStartAssignedTasks() {
    worker.startTask(task1);
    worker.startTask(task2);
    worker.startAssignedTasks();
    verify(perpetualTaskServiceGrpcClient, times(2)).perpetualTaskContext(isA(PerpetualTaskId.class));
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testStopTask() {
    worker.startTask(task1);
    assertThat(worker.getRunningTaskMap()).hasSize(1);
    worker.stopTask(taskId1);
    assertThat(worker.getRunningTaskMap()).hasSize(0);
  }
}
