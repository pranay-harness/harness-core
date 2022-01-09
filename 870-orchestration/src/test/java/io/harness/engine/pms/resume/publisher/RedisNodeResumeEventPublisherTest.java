/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.engine.pms.resume.publisher;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ARCHIT;

import static org.mockito.Mockito.verify;

import io.harness.OrchestrationTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.engine.pms.commons.events.PmsEventSender;
import io.harness.execution.NodeExecution;
import io.harness.plan.PlanNode;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.ExecutionMode;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.TaskExecutableResponse;
import io.harness.pms.contracts.execution.tasks.TaskCategory;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.contracts.resume.NodeResumeEvent;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.events.base.PmsEventCategory;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.rule.Owner;
import io.harness.utils.steps.TestStepParameters;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.PIPELINE)
public class RedisNodeResumeEventPublisherTest extends OrchestrationTestBase {
  @Mock PmsEventSender eventSender;
  @Inject @InjectMocks RedisNodeResumeEventPublisher resumeEventPublisher;

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
    resumeEventPublisher.publishEvent(ResumeMetadata.fromNodeExecution(nodeExecution), new HashMap<>(), false);
    PlanNode planNode = nodeExecution.getNode();
    NodeResumeEvent nodeResumeEvent = NodeResumeEvent.newBuilder()
                                          .setAmbiance(nodeExecution.getAmbiance())
                                          .setExecutionMode(nodeExecution.getMode())
                                          .setStepParameters(nodeExecution.getResolvedStepParametersBytes())
                                          .addAllRefObjects(planNode.getRefObjects())
                                          .setAsyncError(false)
                                          .putAllResponse(new HashMap<>())
                                          .build();

    verify(eventSender)
        .sendEvent(nodeExecution.getAmbiance(), nodeResumeEvent.toByteString(), PmsEventCategory.NODE_RESUME,
            nodeExecution.getNode().getServiceName(), true);
  }
}
