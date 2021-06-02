package io.harness.event;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.pms.contracts.execution.events.OrchestrationEventType.ORCHESTRATION_END;
import static io.harness.rule.OwnerRule.ALEXEI;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.OrchestrationVisualizationTestBase;
import io.harness.beans.OrchestrationGraph;
import io.harness.cache.SpringMongoStore;
import io.harness.category.element.UnitTests;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.execution.PlanExecution;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.plan.ExecutionTriggerInfo;
import io.harness.pms.contracts.plan.TriggerType;
import io.harness.pms.contracts.plan.TriggeredBy;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.rule.Owner;
import io.harness.service.GraphGenerationService;
import io.harness.testlib.RealMongo;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

/**
 * Test class for {@link OrchestrationEndEventHandler}
 */
public class OrchestrationEndEventHandlerTest extends OrchestrationVisualizationTestBase {
  @Inject private SpringMongoStore mongoStore;

  @Inject PlanExecutionService planExecutionService;
  @Inject GraphGenerationService graphGenerationService;

  private OrchestrationEndEventHandler orchestrationEndEventHandler;

  @Before
  public void setUp() {
    ExecutorService executorService = Mockito.mock(ExecutorService.class);
    orchestrationEndEventHandler =
        new OrchestrationEndEventHandler(executorService, planExecutionService, graphGenerationService);
  }

  private static final ExecutionMetadata metadata =
      ExecutionMetadata.newBuilder()
          .setPipelineIdentifier(generateUuid())
          .setTriggerInfo(ExecutionTriggerInfo.newBuilder()
                              .setTriggerType(TriggerType.MANUAL)
                              .setTriggeredBy(TriggeredBy.newBuilder().setIdentifier("admin").build())
                              .build())
          .build();

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  @RealMongo
  public void shouldUpdateGraphWithStatusAndEndTs() {
    PlanExecution planExecution = PlanExecution.builder()
                                      .uuid(generateUuid())
                                      .startTs(System.currentTimeMillis())
                                      .endTs(System.currentTimeMillis())
                                      .status(Status.SUCCEEDED)
                                      .metadata(metadata)
                                      .build();
    planExecutionService.save(planExecution);

    OrchestrationEvent event =
        OrchestrationEvent.builder()
            .ambiance(Ambiance.newBuilder().setPlanExecutionId(planExecution.getUuid()).build())
            .nodeExecutionProto(
                NodeExecutionProto.newBuilder()
                    .setUuid(generateUuid())
                    .setAmbiance(Ambiance.newBuilder().setPlanExecutionId(planExecution.getUuid()).build())
                    .build())
            .eventType(ORCHESTRATION_END)
            .build();

    OrchestrationGraph orchestrationGraph = OrchestrationGraph.builder()
                                                .rootNodeIds(Lists.newArrayList(generateUuid()))
                                                .status(Status.RUNNING)
                                                .startTs(planExecution.getStartTs())
                                                .planExecutionId(planExecution.getUuid())
                                                .cacheKey(planExecution.getUuid())
                                                .cacheContextOrder(System.currentTimeMillis())
                                                .build();
    mongoStore.upsert(orchestrationGraph, Duration.ofDays(10));

    orchestrationEndEventHandler.onEnd(event.getAmbiance());

    OrchestrationGraph updatedGraph = graphGenerationService.getCachedOrchestrationGraph(planExecution.getUuid());
    assertThat(updatedGraph).isNotNull();
    assertThat(updatedGraph.getPlanExecutionId()).isEqualTo(planExecution.getUuid());
    assertThat(updatedGraph.getStartTs()).isEqualTo(planExecution.getStartTs());
    assertThat(updatedGraph.getEndTs()).isEqualTo(planExecution.getEndTs());
    assertThat(updatedGraph.getStatus()).isEqualTo(planExecution.getStatus());
  }
}
