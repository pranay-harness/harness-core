/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.notification.orchestration.handlers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.engine.observers.NodeStartInfo;
import io.harness.execution.NodeExecution;
import io.harness.notification.PipelineEventType;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.notification.NotificationHelper;
import io.harness.rule.Owner;

import java.util.concurrent.ExecutorService;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(PIPELINE)
public class StageStartNotificationHandlerTest extends CategoryTest {
  @Mock ExecutorService executorService;
  @Mock NotificationHelper notificationHelper;
  @InjectMocks StageStartNotificationHandler stageStartNotificationHandler;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testOnNodeStart() {
    Ambiance ambiance = Ambiance.getDefaultInstance();
    long ts = System.currentTimeMillis();
    PlanNodeProto planNodeProto = PlanNodeProto.newBuilder()
                                      .setStepType(StepType.newBuilder().setStepCategory(StepCategory.STAGES).build())
                                      .build();
    NodeExecution nodeExecution = NodeExecution.builder().ambiance(ambiance).node(planNodeProto).build();
    NodeStartInfo nodeStartInfo = NodeStartInfo.builder().nodeExecution(nodeExecution).updatedTs(ts).build();
    stageStartNotificationHandler.onNodeStart(nodeStartInfo);
    verify(notificationHelper, times(0)).sendNotification(any(), any(), any(), any());

    planNodeProto = PlanNodeProto.newBuilder()
                        .setStepType(StepType.newBuilder().setStepCategory(StepCategory.STAGE).build())
                        .build();
    nodeExecution = NodeExecution.builder().ambiance(ambiance).node(planNodeProto).build();
    nodeStartInfo = NodeStartInfo.builder().nodeExecution(nodeExecution).updatedTs(ts).build();
    stageStartNotificationHandler.onNodeStart(nodeStartInfo);
    verify(notificationHelper, times(1)).sendNotification(ambiance, PipelineEventType.STAGE_START, nodeExecution, ts);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetInformExecutorService() {
    assertThat(stageStartNotificationHandler.getInformExecutorService()).isEqualTo(executorService);
  }
}
