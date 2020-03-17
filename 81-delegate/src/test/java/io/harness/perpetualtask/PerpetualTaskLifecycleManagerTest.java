package io.harness.perpetualtask;

import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.protobuf.Any;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.perpetualtask.ecs.EcsPerpetualTaskParams;
import io.harness.perpetualtask.grpc.PerpetualTaskServiceGrpcClient;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class PerpetualTaskLifecycleManagerTest extends CategoryTest {
  private PerpetualTaskLifecycleManager perpetualTaskLifecycleManager;
  private Map<String, PerpetualTaskExecutor> factoryMap = new HashMap<>();
  @Mock private TimeLimiter timeLimiter;
  @Mock private PerpetualTaskServiceGrpcClient perpetualTaskServiceGrpcClient;

  @Mock private PerpetualTaskExecutor perpetualTaskExecutor;

  @Captor private ArgumentCaptor<PerpetualTaskId> perpetualTaskIdArgumentCaptor;
  @Captor private ArgumentCaptor<Instant> instantArgumentCaptor;
  @Captor private ArgumentCaptor<PerpetualTaskResponse> perpetualTaskResponseArgumentCaptor;

  private final String REGION = "us-east-1";
  private final String CLUSTER_ID = "clusterId";
  private final String SETTING_ID = "settingId";
  private final String CLUSTER_NAME = "ecs-ccm-cluster";
  private final String PERPETUAL_TASK_ID = "perpetualTaskId";

  @Before
  public void setUp() throws Exception {
    factoryMap.put("EcsPerpetualTaskParams", perpetualTaskExecutor);

    EcsPerpetualTaskParams ecsPerpetualTaskParams = EcsPerpetualTaskParams.newBuilder()
                                                        .setClusterId(CLUSTER_ID)
                                                        .setRegion(REGION)
                                                        .setSettingId(SETTING_ID)
                                                        .setClusterName(CLUSTER_NAME)
                                                        .build();

    PerpetualTaskParams params =
        PerpetualTaskParams.newBuilder().setCustomizedParams(Any.pack(ecsPerpetualTaskParams)).build();
    PerpetualTaskId perpetualTaskId = PerpetualTaskId.newBuilder().setId(PERPETUAL_TASK_ID).build();
    PerpetualTaskContext taskContext = PerpetualTaskContext.newBuilder().setTaskParams(params).build();
    perpetualTaskLifecycleManager = new PerpetualTaskLifecycleManager(
        perpetualTaskId, taskContext, factoryMap, perpetualTaskServiceGrpcClient, timeLimiter);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testRunOnceWhenResponseIsSuccess() {
    PerpetualTaskResponse perpetualTaskResponse = PerpetualTaskResponse.builder()
                                                      .responseCode(200)
                                                      .perpetualTaskState(PerpetualTaskState.TASK_RUN_SUCCEEDED)
                                                      .build();
    when(perpetualTaskExecutor.runOnce(any(), any(), any())).thenReturn(perpetualTaskResponse);
    perpetualTaskLifecycleManager.call();
    verify(perpetualTaskServiceGrpcClient)
        .publishHeartbeat(perpetualTaskIdArgumentCaptor.capture(), instantArgumentCaptor.capture(),
            perpetualTaskResponseArgumentCaptor.capture());
    assertThat(perpetualTaskIdArgumentCaptor.getValue().getId()).isEqualTo(PERPETUAL_TASK_ID);
    assertThat(perpetualTaskResponseArgumentCaptor.getValue()).isEqualTo(perpetualTaskResponse);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testRunOnceWhenTimeout() {
    PerpetualTaskResponse perpetualTaskResponse = PerpetualTaskResponse.builder()
                                                      .responseCode(408)
                                                      .perpetualTaskState(PerpetualTaskState.TASK_RUN_FAILED)
                                                      .responseMessage(PerpetualTaskState.TASK_RUN_FAILED.name())
                                                      .build();
    when(perpetualTaskExecutor.runOnce(any(), any(), any())).thenThrow(UncheckedTimeoutException.class);
    perpetualTaskLifecycleManager.call();
    verify(perpetualTaskServiceGrpcClient)
        .publishHeartbeat(perpetualTaskIdArgumentCaptor.capture(), instantArgumentCaptor.capture(),
            perpetualTaskResponseArgumentCaptor.capture());
    assertThat(perpetualTaskIdArgumentCaptor.getValue().getId()).isEqualTo(PERPETUAL_TASK_ID);
    assertThat(perpetualTaskResponseArgumentCaptor.getValue()).isEqualTo(perpetualTaskResponse);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testRunOnceWhenExceptionWhileExecuting() {
    PerpetualTaskResponse perpetualTaskResponse = PerpetualTaskResponse.builder()
                                                      .responseCode(500)
                                                      .perpetualTaskState(PerpetualTaskState.TASK_RUN_FAILED)
                                                      .responseMessage(PerpetualTaskState.TASK_RUN_FAILED.name())
                                                      .build();
    when(perpetualTaskExecutor.runOnce(any(), any(), any())).thenThrow(Exception.class);
    perpetualTaskLifecycleManager.call();
    verify(perpetualTaskServiceGrpcClient)
        .publishHeartbeat(perpetualTaskIdArgumentCaptor.capture(), instantArgumentCaptor.capture(),
            perpetualTaskResponseArgumentCaptor.capture());
    assertThat(perpetualTaskIdArgumentCaptor.getValue().getId()).isEqualTo(PERPETUAL_TASK_ID);
    assertThat(perpetualTaskResponseArgumentCaptor.getValue()).isEqualTo(perpetualTaskResponse);
  }
}
