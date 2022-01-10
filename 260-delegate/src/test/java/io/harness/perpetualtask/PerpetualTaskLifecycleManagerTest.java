/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.perpetualtask;

import static io.harness.rule.OwnerRule.HITESH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.perpetualtask.ecs.EcsPerpetualTaskParams;
import io.harness.perpetualtask.grpc.PerpetualTaskServiceGrpcClient;
import io.harness.rule.Owner;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.protobuf.Any;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(PerpetualTaskLifecycleManager.class)
public class PerpetualTaskLifecycleManagerTest extends CategoryTest {
  private PerpetualTaskLifecycleManager perpetualTaskLifecycleManager;
  private final Map<String, PerpetualTaskExecutor> factoryMap = new HashMap<>();
  @Mock private TimeLimiter timeLimiter;
  @Mock private PerpetualTaskServiceGrpcClient perpetualTaskServiceGrpcClient;

  @Mock private PerpetualTaskExecutor perpetualTaskExecutor;

  @Captor private ArgumentCaptor<PerpetualTaskId> perpetualTaskIdArgumentCaptor;
  @Captor private ArgumentCaptor<Instant> instantArgumentCaptor;
  @Captor private ArgumentCaptor<PerpetualTaskResponse> perpetualTaskResponseArgumentCaptor;

  private final String PERPETUAL_TASK_ID = "perpetualTaskId";

  @Mock private AtomicInteger currentlyExecutingPerpetualTasksCount;

  @Before
  public void setUp() throws Exception {
    factoryMap.put("EcsPerpetualTaskParams", perpetualTaskExecutor);

    String REGION = "us-east-1";
    String CLUSTER_ID = "clusterId";
    String SETTING_ID = "settingId";
    String CLUSTER_NAME = "ecs-ccm-cluster";
    EcsPerpetualTaskParams ecsPerpetualTaskParams = EcsPerpetualTaskParams.newBuilder()
                                                        .setClusterId(CLUSTER_ID)
                                                        .setRegion(REGION)
                                                        .setSettingId(SETTING_ID)
                                                        .setClusterName(CLUSTER_NAME)
                                                        .build();

    PerpetualTaskExecutionParams params =
        PerpetualTaskExecutionParams.newBuilder().setCustomizedParams(Any.pack(ecsPerpetualTaskParams)).build();
    PerpetualTaskId perpetualTaskId = PerpetualTaskId.newBuilder().setId(PERPETUAL_TASK_ID).build();
    PerpetualTaskExecutionContext taskContext =
        PerpetualTaskExecutionContext.newBuilder().setTaskParams(params).build();
    perpetualTaskLifecycleManager = new PerpetualTaskLifecycleManager(perpetualTaskId, taskContext, factoryMap,
        perpetualTaskServiceGrpcClient, timeLimiter, currentlyExecutingPerpetualTasksCount);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testRunOnceWhenResponseIsSuccess() {
    PerpetualTaskResponse perpetualTaskResponse = PerpetualTaskResponse.builder().responseCode(200).build();
    when(perpetualTaskExecutor.runOnce(any(), any(), any())).thenReturn(perpetualTaskResponse);
    when(currentlyExecutingPerpetualTasksCount.get()).thenReturn(1);
    perpetualTaskLifecycleManager.call();
    verify(perpetualTaskServiceGrpcClient)
        .heartbeat(perpetualTaskIdArgumentCaptor.capture(), instantArgumentCaptor.capture(),
            perpetualTaskResponseArgumentCaptor.capture());
    assertThat(perpetualTaskIdArgumentCaptor.getValue().getId()).isEqualTo(PERPETUAL_TASK_ID);
    assertThat(perpetualTaskResponseArgumentCaptor.getValue()).isEqualTo(perpetualTaskResponse);

    verify(currentlyExecutingPerpetualTasksCount, times(1)).getAndIncrement();
    verify(currentlyExecutingPerpetualTasksCount, times(1)).getAndDecrement();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testRunOnceWhenTimeout() {
    PerpetualTaskResponse perpetualTaskResponse =
        PerpetualTaskResponse.builder().responseCode(408).responseMessage("failed").build();
    when(perpetualTaskExecutor.runOnce(any(), any(), any())).thenThrow(UncheckedTimeoutException.class);
    when(currentlyExecutingPerpetualTasksCount.get()).thenReturn(1);
    perpetualTaskLifecycleManager.call();
    verify(perpetualTaskServiceGrpcClient)
        .heartbeat(perpetualTaskIdArgumentCaptor.capture(), instantArgumentCaptor.capture(),
            perpetualTaskResponseArgumentCaptor.capture());
    assertThat(perpetualTaskIdArgumentCaptor.getValue().getId()).isEqualTo(PERPETUAL_TASK_ID);
    assertThat(perpetualTaskResponseArgumentCaptor.getValue()).isEqualTo(perpetualTaskResponse);

    verify(currentlyExecutingPerpetualTasksCount, times(1)).getAndIncrement();
    verify(currentlyExecutingPerpetualTasksCount, times(1)).getAndDecrement();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testRunOnceWhenExceptionWhileExecuting() {
    PerpetualTaskResponse perpetualTaskResponse =
        PerpetualTaskResponse.builder().responseCode(500).responseMessage("failed").build();
    when(perpetualTaskExecutor.runOnce(any(), any(), any())).thenThrow(Exception.class);
    when(currentlyExecutingPerpetualTasksCount.get()).thenReturn(1);
    perpetualTaskLifecycleManager.call();
    verify(perpetualTaskServiceGrpcClient)
        .heartbeat(perpetualTaskIdArgumentCaptor.capture(), instantArgumentCaptor.capture(),
            perpetualTaskResponseArgumentCaptor.capture());
    assertThat(perpetualTaskIdArgumentCaptor.getValue().getId()).isEqualTo(PERPETUAL_TASK_ID);
    assertThat(perpetualTaskResponseArgumentCaptor.getValue()).isEqualTo(perpetualTaskResponse);

    verify(currentlyExecutingPerpetualTasksCount, times(1)).getAndIncrement();
    verify(currentlyExecutingPerpetualTasksCount, times(1)).getAndDecrement();
  }
}
