package io.harness.engine.executions;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ARCHIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.OrchestrationTestBase;
import io.harness.category.element.UnitTests;
import io.harness.engine.interrupts.InterruptPackage;
import io.harness.pms.contracts.advisers.InterventionWaitAdvise;
import io.harness.pms.contracts.commons.RepairActionCode;
import io.harness.pms.contracts.interrupts.InterruptType;
import io.harness.rule.Owner;
import io.harness.testlib.RealMongo;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class InterventionWaitTimeoutCallbackTest extends OrchestrationTestBase {
  InterventionWaitTimeoutCallback interventionWaitTimeoutCallback;

  private static final String PLAN_EXECUTION_ID = generateUuid();
  private static final String NODE_EXECUTION_ID = generateUuid();

  @Before
  public void setup() {
    interventionWaitTimeoutCallback = new InterventionWaitTimeoutCallback(PLAN_EXECUTION_ID, NODE_EXECUTION_ID);
  }

  @Test
  @RealMongo
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void shouldTestGetInterruptPackage() {
    InterventionWaitAdvise advise =
        InterventionWaitAdvise.newBuilder().setRepairActionCode(RepairActionCode.IGNORE).build();
    InterruptPackage interruptPackage = interventionWaitTimeoutCallback.getInterruptPackage(advise, "");
    assertThat(interruptPackage).isNotNull();
    assertThat(interruptPackage.getInterruptType()).isEqualTo(InterruptType.IGNORE);
    assertThat(interruptPackage.getNodeExecutionId()).isEqualTo(NODE_EXECUTION_ID);
    assertThat(interruptPackage.getPlanExecutionId()).isEqualTo(PLAN_EXECUTION_ID);
  }
}