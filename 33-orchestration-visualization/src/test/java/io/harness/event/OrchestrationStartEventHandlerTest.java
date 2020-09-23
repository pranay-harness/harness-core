package io.harness.event;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.execution.events.OrchestrationEventType.ORCHESTRATION_START;
import static io.harness.rule.OwnerRule.ALEXEI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.inject.Inject;

import io.harness.OrchestrationVisualizationTest;
import io.harness.ambiance.Ambiance;
import io.harness.beans.OrchestrationGraphInternal;
import io.harness.category.element.UnitTests;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.PlanExecution;
import io.harness.execution.events.OrchestrationEvent;
import io.harness.execution.status.Status;
import io.harness.rule.Owner;
import io.harness.service.GraphGenerationService;
import io.harness.testlib.RealMongo;
import org.awaitility.Awaitility;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.concurrent.TimeUnit;

/**
 * Test class for {@link OrchestrationStartEventHandler}
 */
public class OrchestrationStartEventHandlerTest extends OrchestrationVisualizationTest {
  @Inject private PlanExecutionService planExecutionService;
  @Inject private GraphGenerationService graphGenerationService;
  @Inject private OrchestrationStartEventHandler orchestrationStartEventHandler;

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  @RealMongo
  public void shouldThrowInvalidRequestException() {
    String planExecutionId = generateUuid();
    OrchestrationEvent event = OrchestrationEvent.builder()
                                   .ambiance(Ambiance.builder().planExecutionId(planExecutionId).build())
                                   .eventType(ORCHESTRATION_START)
                                   .build();

    assertThatThrownBy(() -> orchestrationStartEventHandler.handleEvent(event))
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

    OrchestrationEvent event = OrchestrationEvent.builder()
                                   .ambiance(Ambiance.builder().planExecutionId(planExecution.getUuid()).build())
                                   .eventType(ORCHESTRATION_START)
                                   .build();

    orchestrationStartEventHandler.handleEvent(event);

    Awaitility.await().atMost(2, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS).until(() -> {
      OrchestrationGraphInternal graphInternal =
          graphGenerationService.getCachedOrchestrationGraphInternal(planExecution.getUuid());
      return graphInternal != null;
    });

    OrchestrationGraphInternal orchestrationGraphInternal =
        graphGenerationService.getCachedOrchestrationGraphInternal(planExecution.getUuid());

    assertThat(orchestrationGraphInternal).isNotNull();
    assertThat(orchestrationGraphInternal.getPlanExecutionId()).isEqualTo(planExecution.getUuid());
    assertThat(orchestrationGraphInternal.getStartTs()).isEqualTo(planExecution.getStartTs());
    assertThat(orchestrationGraphInternal.getEndTs()).isNull();
    assertThat(orchestrationGraphInternal.getStatus()).isEqualTo(planExecution.getStatus());
  }
}
