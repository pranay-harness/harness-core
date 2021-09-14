/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.engine.facilitation;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.OrchestrationTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.facilitation.facilitator.publisher.FacilitateEventPublisher;
import io.harness.engine.facilitation.facilitator.sync.SyncFacilitator;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(HarnessTeam.PIPELINE)
public class FacilitationHelperTest extends OrchestrationTestBase {
  @Mock private OrchestrationEngine orchestrationEngine;
  @Mock private FacilitateEventPublisher facilitateEventPublisher;
  @Inject @InjectMocks private FacilitationHelper facilitationHelper;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testFacilitateExecutionCustom() {
    NodeExecution nodeExecution =
        NodeExecution.builder()
            .uuid(generateUuid())
            .ambiance(Ambiance.newBuilder().setPlanExecutionId(generateUuid()).build())
            .status(Status.QUEUED)
            .node(PlanNodeProto.newBuilder()
                      .setUuid(generateUuid())
                      .setStepType(StepType.newBuilder().setType("DUMMY").setStepCategory(StepCategory.STEP).build())
                      .addFacilitatorObtainments(FacilitatorObtainment.newBuilder()
                                                     .setType(FacilitatorType.newBuilder().setType("CUSTOM").build())
                                                     .build())
                      .build())
            .startTs(System.currentTimeMillis())
            .build();
    facilitationHelper.facilitateExecution(nodeExecution);

    verify(facilitateEventPublisher, times(1)).publishEvent(eq(nodeExecution.getUuid()));
    verify(orchestrationEngine, times(0)).facilitateExecution(eq(nodeExecution.getUuid()), any());
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testFacilitateExecutionCore() {
    NodeExecution nodeExecution =
        NodeExecution.builder()
            .uuid(generateUuid())
            .ambiance(Ambiance.newBuilder().setPlanExecutionId(generateUuid()).build())
            .status(Status.QUEUED)
            .node(PlanNodeProto.newBuilder()
                      .setUuid(generateUuid())
                      .setStepType(StepType.newBuilder().setType("DUMMY").setStepCategory(StepCategory.STEP).build())
                      .addFacilitatorObtainments(
                          FacilitatorObtainment.newBuilder().setType(SyncFacilitator.FACILITATOR_TYPE).build())
                      .build())
            .startTs(System.currentTimeMillis())
            .build();
    facilitationHelper.facilitateExecution(nodeExecution);

    verify(facilitateEventPublisher, times(0)).publishEvent(eq(nodeExecution.getUuid()));
    verify(orchestrationEngine, times(1)).facilitateExecution(eq(nodeExecution.getUuid()), any());
  }
}
