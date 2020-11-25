package io.harness.advisers.abort;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ALEXEI;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.OrchestrationTestBase;
import io.harness.adviser.Advise;
import io.harness.adviser.AdvisingEvent;
import io.harness.adviser.advise.EndPlanAdvise;
import io.harness.ambiance.Ambiance;
import io.harness.category.element.UnitTests;
import io.harness.pms.ambiance.Level;
import io.harness.pms.execution.Status;
import io.harness.pms.steps.StepType;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;
import io.harness.utils.AmbianceTestUtils;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class OnAbortAdviserTest extends OrchestrationTestBase {
  public static final String NODE_EXECUTION_ID = generateUuid();
  public static final String NODE_SETUP_ID = generateUuid();
  public static final String NODE_IDENTIFIER = "DUMMY";
  public static final StepType DUMMY_STEP_TYPE = StepType.newBuilder().setType(NODE_IDENTIFIER).build();

  @Inject OnAbortAdviser onAbortAdviser;

  private Ambiance ambiance;

  @Before
  public void setup() {
    ambiance = AmbianceTestUtils.buildAmbiance();
    ambiance.addLevel(Level.newBuilder()
                          .setSetupId(NODE_SETUP_ID)
                          .setRuntimeId(NODE_EXECUTION_ID)
                          .setIdentifier(NODE_IDENTIFIER)
                          .setStepType(DUMMY_STEP_TYPE)
                          .build());
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestOnAdviseEvent() {
    AdvisingEvent advisingEvent = AdvisingEvent.builder().ambiance(ambiance).toStatus(Status.FAILED).build();
    Advise advise = onAbortAdviser.onAdviseEvent(advisingEvent);
    assertThat(advise).isInstanceOf(EndPlanAdvise.class);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestCanAdvise() {
    AdvisingEvent advisingEvent = AdvisingEvent.builder().ambiance(ambiance).toStatus(Status.ABORTED).build();
    boolean canAdvise = onAbortAdviser.canAdvise(advisingEvent);
    assertThat(canAdvise).isTrue();
  }
}
