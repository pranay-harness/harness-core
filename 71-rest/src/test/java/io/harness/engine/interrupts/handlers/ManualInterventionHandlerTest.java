package io.harness.engine.interrupts.handlers;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.execution.status.Status.INTERVENTION_WAITING;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.beans.EmbeddedUser;
import io.harness.category.element.UnitTests;
import io.harness.engine.OrchestrationService;
import io.harness.engine.PlanRepo;
import io.harness.engine.interrupts.InterruptTestHelper;
import io.harness.engine.interrupts.steps.SimpleAsyncStep;
import io.harness.execution.PlanExecution;
import io.harness.registries.state.StepRegistry;
import io.harness.rule.Owner;
import io.harness.testlib.RealMongo;
import io.harness.waiter.OrchestrationNotifyEventListener;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.rules.Listeners;

@Listeners(OrchestrationNotifyEventListener.class)
public class ManualInterventionHandlerTest extends WingsBaseTest {
  @Inject private OrchestrationService orchestrationService;
  @Inject private InterruptTestHelper interruptTestHelper;
  @Inject private StepRegistry stepRegistry;

  private static final EmbeddedUser EMBEDDED_USER = new EmbeddedUser(generateUuid(), PRASHANT, PRASHANT);

  @Before
  public void setUp() {
    stepRegistry.register(SimpleAsyncStep.STEP_TYPE, SimpleAsyncStep.class);
  }

  @Test
  @RealMongo
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestHandleInterrupt() {
    PlanExecution execution = orchestrationService.startExecution(PlanRepo.planWithFailure(),
        ImmutableMap.of("accountId", generateUuid(), "appId", generateUuid()), EMBEDDED_USER);
    interruptTestHelper.waitForPlanStatus(execution.getUuid(), INTERVENTION_WAITING);
    assertThat(execution).isNotNull();
  }
}
