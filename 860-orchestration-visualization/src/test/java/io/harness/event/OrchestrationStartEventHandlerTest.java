/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.pms.contracts.execution.events.OrchestrationEventType.ORCHESTRATION_START;
import static io.harness.rule.OwnerRule.ALEXEI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.OrchestrationVisualizationTestBase;
import io.harness.beans.OrchestrationGraph;
import io.harness.category.element.UnitTests;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.engine.observers.beans.OrchestrationStartInfo;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.PlanExecution;
import io.harness.execution.PlanExecutionMetadata;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.rule.Owner;
import io.harness.service.GraphGenerationService;
import io.harness.testlib.RealMongo;

import com.google.inject.Inject;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test class for {@link OrchestrationStartEventHandler}
 */
public class OrchestrationStartEventHandlerTest extends OrchestrationVisualizationTestBase {
  @Inject private PlanExecutionService planExecutionService;
  @Inject private GraphGenerationService graphGenerationService;
  @Inject private OrchestrationStartEventHandler orchestrationStartEventHandler;

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  @RealMongo
  public void shouldThrowInvalidRequestException() {
    String planExecutionId = generateUuid();
    PlanExecutionMetadata planExecutionMetadata =
        PlanExecutionMetadata.builder().planExecutionId(planExecutionId).build();
    OrchestrationEvent event = OrchestrationEvent.builder()
                                   .ambiance(Ambiance.newBuilder().setPlanExecutionId(planExecutionId).build())
                                   .eventType(ORCHESTRATION_START)
                                   .build();

    assertThatThrownBy(()
                           -> orchestrationStartEventHandler.onStart(OrchestrationStartInfo.builder()
                                                                         .ambiance(event.getAmbiance())
                                                                         .planExecutionMetadata(planExecutionMetadata)
                                                                         .build()))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  @RealMongo
  public void shouldSaveCachedGraph() {
    PlanExecution planExecution =
        PlanExecution.builder().uuid(generateUuid()).startTs(System.currentTimeMillis()).status(Status.RUNNING).build();
    planExecutionService.save(planExecution);

    PlanExecutionMetadata planExecutionMetadata =
        PlanExecutionMetadata.builder().planExecutionId(planExecution.getUuid()).build();

    OrchestrationEvent event = OrchestrationEvent.builder()
                                   .ambiance(Ambiance.newBuilder().setPlanExecutionId(planExecution.getUuid()).build())
                                   .eventType(ORCHESTRATION_START)
                                   .build();

    orchestrationStartEventHandler.onStart(OrchestrationStartInfo.builder()
                                               .ambiance(event.getAmbiance())
                                               .planExecutionMetadata(planExecutionMetadata)
                                               .build());

    Awaitility.await().atMost(2, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS).until(() -> {
      OrchestrationGraph graphInternal = graphGenerationService.getCachedOrchestrationGraph(planExecution.getUuid());
      return graphInternal != null;
    });

    OrchestrationGraph orchestrationGraph = graphGenerationService.getCachedOrchestrationGraph(planExecution.getUuid());

    assertThat(orchestrationGraph).isNotNull();
    assertThat(orchestrationGraph.getPlanExecutionId()).isEqualTo(planExecution.getUuid());
    assertThat(orchestrationGraph.getStartTs()).isEqualTo(planExecution.getStartTs());
    assertThat(orchestrationGraph.getEndTs()).isNull();
    assertThat(orchestrationGraph.getStatus()).isEqualTo(planExecution.getStatus());
  }
}
