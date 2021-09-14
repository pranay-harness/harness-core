/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.engine.progress.publisher;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ARCHIT;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.OrchestrationTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.pms.commons.events.PmsEventSender;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.ExecutionMode;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.TaskExecutableResponse;
import io.harness.pms.contracts.execution.tasks.TaskCategory;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.contracts.progress.ProgressEvent;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.events.base.PmsEventCategory;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.rule.Owner;
import io.harness.tasks.BinaryResponseData;
import io.harness.utils.steps.TestStepParameters;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.PIPELINE)
public class RedisProgressEventPublisherTest extends OrchestrationTestBase {
  @Mock NodeExecutionService nodeExecutionService;
  @Mock PmsEventSender eventSender;
  @Inject @InjectMocks RedisProgressEventPublisher redisProgressEventPublisher;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testIfStepParametersSentAreResolvedOne() {
    StepParameters sectionStepParams = TestStepParameters.builder().param("DummySection").build();
    StepParameters resolvedSectionStepParams = TestStepParameters.builder().param("ResolvedDummySection").build();
    NodeExecution nodeExecution =
        NodeExecution.builder()
            .uuid(generateUuid())
            .ambiance(Ambiance.newBuilder().setPlanExecutionId(generateUuid()).build())
            .status(Status.RUNNING)
            .mode(ExecutionMode.ASYNC)
            .node(PlanNodeProto.newBuilder()
                      .setUuid(generateUuid())
                      .setStepType(StepType.newBuilder().setType("DUMMY").setStepCategory(StepCategory.STEP).build())
                      .setStepParameters(RecastOrchestrationUtils.toJson(sectionStepParams))
                      .setServiceName("DUMMY")
                      .build())
            .executableResponse(ExecutableResponse.newBuilder()
                                    .setTask(TaskExecutableResponse.newBuilder()
                                                 .setTaskId(generateUuid())
                                                 .setTaskCategory(TaskCategory.UNKNOWN_CATEGORY)
                                                 .build())
                                    .build())
            .resolvedStepParameters(resolvedSectionStepParams)
            .interruptHistories(new ArrayList<>())
            .startTs(System.currentTimeMillis())
            .build();
    when(nodeExecutionService.get(nodeExecution.getUuid())).thenReturn(nodeExecution);
    redisProgressEventPublisher.publishEvent(
        nodeExecution.getUuid(), BinaryResponseData.builder().data(nodeExecution.getNode().toByteArray()).build());

    ProgressEvent progressEvent = ProgressEvent.newBuilder()
                                      .setAmbiance(nodeExecution.getAmbiance())
                                      .setExecutionMode(nodeExecution.getMode())
                                      .setStepParameters(nodeExecution.getResolvedStepParametersBytes())
                                      .setProgressBytes(ByteString.copyFrom(nodeExecution.getNode().toByteArray()))
                                      .build();

    verify(eventSender)
        .sendEvent(nodeExecution.getAmbiance(), progressEvent.toByteString(), PmsEventCategory.PROGRESS_EVENT,
            nodeExecution.getNode().getServiceName(), false);
  }
}
