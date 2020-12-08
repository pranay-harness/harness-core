package io.harness.engine.advise.handlers;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ALEXEI;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.OrchestrationTestBase;
import io.harness.category.element.UnitTests;
import io.harness.pms.advisers.AdviserResponse;
import io.harness.pms.advisers.MarkSuccessAdvise;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.ambiance.Level;
import io.harness.pms.execution.Status;
import io.harness.pms.sdk.core.adviser.AdvisingEvent;
import io.harness.pms.sdk.core.adviser.marksuccess.OnMarkSuccessAdviser;
import io.harness.pms.sdk.core.adviser.marksuccess.OnMarkSuccessAdviserParameters;
import io.harness.pms.steps.StepType;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class OnMarkSuccessAdviserTest extends OrchestrationTestBase {
  public static final String NODE_EXECUTION_ID = generateUuid();
  public static final String NODE_SETUP_ID = generateUuid();
  public static final String NODE_IDENTIFIER = "DUMMY";
  public static final StepType DUMMY_STEP_TYPE = StepType.newBuilder().setType(NODE_IDENTIFIER).build();

  @Inject OnMarkSuccessAdviser onMarkSuccessAdviser;
  @Inject KryoSerializer kryoSerializer;

  private Ambiance ambiance;

  @Before
  public void setup() {
    ambiance = Ambiance.newBuilder()
                   .addLevels(Level.newBuilder()
                                  .setSetupId(NODE_SETUP_ID)
                                  .setRuntimeId(NODE_EXECUTION_ID)
                                  .setIdentifier(NODE_IDENTIFIER)
                                  .setStepType(DUMMY_STEP_TYPE)
                                  .build())
                   .build();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestOnAdviseEvent() {
    String nextNodeId = generateUuid();
    AdvisingEvent advisingEvent = AdvisingEvent.builder()
                                      .ambiance(ambiance)
                                      .toStatus(Status.SUCCEEDED)
                                      .adviserParameters(kryoSerializer.asBytes(
                                          OnMarkSuccessAdviserParameters.builder().nextNodeId(nextNodeId).build()))
                                      .build();
    AdviserResponse adviserResponse = onMarkSuccessAdviser.onAdviseEvent(advisingEvent);
    assertThat(adviserResponse).isInstanceOf(AdviserResponse.class);
    MarkSuccessAdvise nextStepAdvise = adviserResponse.getMarkSuccessAdvise();
    assertThat(nextStepAdvise.getNextNodeId()).isEqualTo(nextNodeId);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestCanAdvise() {
    AdvisingEvent advisingEvent = AdvisingEvent.builder().ambiance(ambiance).toStatus(Status.ABORTED).build();
    boolean canAdvise = onMarkSuccessAdviser.canAdvise(advisingEvent);
    assertThat(canAdvise).isTrue();
  }
}
