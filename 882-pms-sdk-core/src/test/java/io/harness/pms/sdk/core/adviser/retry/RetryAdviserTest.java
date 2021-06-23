package io.harness.pms.sdk.core.adviser.retry;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.advisers.AdviseType;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.advisers.RetryAdvise;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.commons.RepairActionCode;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.failure.FailureType;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.AmbianceTestUtils;
import io.harness.pms.sdk.core.PmsSdkCoreTestBase;
import io.harness.pms.sdk.core.adviser.AdvisingEvent;
import io.harness.pms.sdk.core.adviser.AdvisingEvent.AdvisingEventBuilder;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.util.Arrays;
import java.util.EnumSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

@OwnedBy(HarnessTeam.PIPELINE)
public class RetryAdviserTest extends PmsSdkCoreTestBase {
  public static final String DUMMY_NODE_ID = generateUuid();
  public static final String NODE_EXECUTION_ID = generateUuid();
  public static final String NODE_SETUP_ID = generateUuid();
  public static final String NODE_NAME = generateUuid();
  public static final String NODE_IDENTIFIER = "DUMMY";
  public static final StepType DUMMY_STEP_TYPE = StepType.newBuilder().setType("DUMMY").build();

  @InjectMocks @Inject RetryAdviser retryAdviser;

  @Inject KryoSerializer kryoSerializer;

  private Ambiance ambiance;

  @Before
  public void setup() {
    ambiance = AmbianceTestUtils.buildAmbiance();
    ambiance = ambiance.toBuilder()
                   .addLevels(Level.newBuilder()
                                  .setSetupId(NODE_SETUP_ID)
                                  .setRuntimeId(NODE_EXECUTION_ID)
                                  .setIdentifier(NODE_IDENTIFIER)
                                  .setStepType(DUMMY_STEP_TYPE)
                                  .build())
                   .build();
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestValidStatus() {
    AdvisingEvent advisingEvent = AdvisingEvent.builder()
                                      .ambiance(ambiance)
                                      .toStatus(Status.FAILED)
                                      .adviserParameters(kryoSerializer.asBytes(getRetryParamsWithIgnore()))
                                      .build();
    AdviserResponse adviserResponse = retryAdviser.onAdviseEvent(advisingEvent);

    assertThat(adviserResponse.getType()).isEqualTo(AdviseType.RETRY);
    assertThat(adviserResponse.getRetryAdvise()).isNotNull();
    RetryAdvise retryAdvise = adviserResponse.getRetryAdvise();
    assertThat(retryAdvise.getRetryNodeExecutionId()).isEqualTo(NODE_EXECUTION_ID);
    assertThat(retryAdvise.getWaitInterval()).isEqualTo(2);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestLastWaitInterval() {
    AdvisingEvent advisingEvent =
        AdvisingEvent.<io.harness.pms.sdk.core.adviser.retry.RetryAdviserParameters>builder()
            .ambiance(ambiance)
            .retryIds(Arrays.asList(generateUuid(), generateUuid(), generateUuid(), generateUuid()))
            .toStatus(Status.FAILED)
            .adviserParameters(kryoSerializer.asBytes(getRetryParamsWithIgnore()))
            .build();
    AdviserResponse adviserResponse = retryAdviser.onAdviseEvent(advisingEvent);

    assertThat(adviserResponse.getType()).isEqualTo(AdviseType.RETRY);
    assertThat(adviserResponse.getRetryAdvise()).isNotNull();
    RetryAdvise retryAdvise = adviserResponse.getRetryAdvise();
    assertThat(retryAdvise.getRetryNodeExecutionId()).isEqualTo(NODE_EXECUTION_ID);
    assertThat(retryAdvise.getWaitInterval()).isEqualTo(5);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestAfterRetryStatus() {
    AdvisingEvent advisingEvent =
        AdvisingEvent.builder()
            .ambiance(ambiance)
            .retryIds(Arrays.asList(generateUuid(), generateUuid(), generateUuid(), generateUuid(), generateUuid()))
            .toStatus(Status.FAILED)
            .adviserParameters(kryoSerializer.asBytes(getRetryParamsWithIgnore()))
            .build();
    AdviserResponse adviserResponse = retryAdviser.onAdviseEvent(advisingEvent);

    assertThat(adviserResponse.getType()).isEqualTo(AdviseType.IGNORE_FAILURE);
    assertThat(adviserResponse.getIgnoreFailureAdvise()).isNotNull();
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestCanAdvise() {
    AdvisingEventBuilder advisingEventBuilder =
        AdvisingEvent.builder()
            .ambiance(ambiance)
            .failureInfo(FailureInfo.newBuilder()
                             .setErrorMessage("Auth Error")
                             .addAllFailureTypes(EnumSet.of(FailureType.AUTHENTICATION_FAILURE))
                             .build())
            .toStatus(Status.FAILED)
            .adviserParameters(kryoSerializer.asBytes(getRetryParamsWithIgnore()));

    AdvisingEvent authFailEvent = advisingEventBuilder.build();
    boolean canAdvise = retryAdviser.canAdvise(authFailEvent);
    assertThat(canAdvise).isTrue();

    AdvisingEvent appFailEvent = advisingEventBuilder
                                     .failureInfo(FailureInfo.newBuilder()
                                                      .setErrorMessage("Application Error")
                                                      .addAllFailureTypes(EnumSet.of(FailureType.APPLICATION_FAILURE))
                                                      .build())
                                     .build();
    canAdvise = retryAdviser.canAdvise(appFailEvent);
    assertThat(canAdvise).isFalse();
  }

  private static io.harness.pms.sdk.core.adviser.retry.RetryAdviserParameters getRetryParamsWithIgnore() {
    return RetryAdviserParameters.builder()
        .retryCount(5)
        .waitIntervalList(ImmutableList.of(2, 5))
        .repairActionCodeAfterRetry(RepairActionCode.IGNORE)
        .nextNodeId(DUMMY_NODE_ID)
        .applicableFailureTypes(EnumSet.of(FailureType.AUTHENTICATION_FAILURE))
        .build();
  }
}
