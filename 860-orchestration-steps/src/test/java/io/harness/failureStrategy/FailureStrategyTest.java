package io.harness.failureStrategy;

import static io.harness.rule.OwnerRule.PRASHANTSHARMA;
import static io.harness.yaml.core.failurestrategy.NGFailureType.AUTHENTICATION_ERROR;
import static io.harness.yaml.core.failurestrategy.NGFailureType.AUTHORIZATION_ERROR;
import static io.harness.yaml.core.failurestrategy.NGFailureType.CONNECTIVITY_ERROR;
import static io.harness.yaml.core.failurestrategy.NGFailureType.TIMEOUT_ERROR;
import static io.harness.yaml.core.failurestrategy.NGFailureType.VERIFICATION_ERROR;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.plancreator.steps.FailureStrategiesUtils;
import io.harness.pms.contracts.execution.failure.FailureType;
import io.harness.rule.Owner;
import io.harness.yaml.core.failurestrategy.FailureStrategyActionConfig;
import io.harness.yaml.core.failurestrategy.FailureStrategyConfig;
import io.harness.yaml.core.failurestrategy.NGFailureType;
import io.harness.yaml.core.failurestrategy.OnFailureConfig;
import io.harness.yaml.core.failurestrategy.abort.AbortFailureActionConfig;
import io.harness.yaml.core.failurestrategy.ignore.IgnoreFailureActionConfig;
import io.harness.yaml.core.failurestrategy.marksuccess.MarkAsSuccessFailureActionConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class FailureStrategyTest extends CategoryTest {
  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testprioritySameErrorsDifferentAction() {
    NGFailureType sameErrorForStepStageAndStepGroup = AUTHENTICATION_ERROR;
    NGFailureType sameErrorForStageAndStepGroup = AUTHORIZATION_ERROR;
    NGFailureType ErrorOnlyForStage = TIMEOUT_ERROR;

    IgnoreFailureActionConfig ExpectedStepAction = IgnoreFailureActionConfig.builder().build();
    MarkAsSuccessFailureActionConfig ExpectedStepStageAction = MarkAsSuccessFailureActionConfig.builder().build();
    AbortFailureActionConfig ExpectedStageAction = AbortFailureActionConfig.builder().build();

    List<FailureStrategyConfig> stageFailureStrategies = new ArrayList<>();
    stageFailureStrategies.add(FailureStrategyConfig.builder()
                                   .onFailure(OnFailureConfig.builder()
                                                  .errors(Collections.singletonList(sameErrorForStepStageAndStepGroup))
                                                  .action(AbortFailureActionConfig.builder().build())
                                                  .build())
                                   .build());
    stageFailureStrategies.add(FailureStrategyConfig.builder()
                                   .onFailure(OnFailureConfig.builder()
                                                  .errors(Collections.singletonList(sameErrorForStageAndStepGroup))
                                                  .action(AbortFailureActionConfig.builder().build())
                                                  .build())
                                   .build());
    stageFailureStrategies.add(FailureStrategyConfig.builder()
                                   .onFailure(OnFailureConfig.builder()
                                                  .errors(Collections.singletonList(ErrorOnlyForStage))
                                                  .action(AbortFailureActionConfig.builder().build())
                                                  .build())
                                   .build());

    List<FailureStrategyConfig> stepFailureStrategies = new ArrayList<>();
    stepFailureStrategies.add(FailureStrategyConfig.builder()
                                  .onFailure(OnFailureConfig.builder()
                                                 .errors(Collections.singletonList(sameErrorForStepStageAndStepGroup))
                                                 .action(IgnoreFailureActionConfig.builder().build())
                                                 .build())
                                  .build());

    List<FailureStrategyConfig> stepGroupFailureStrategies = new ArrayList<>();
    stepGroupFailureStrategies.add(
        FailureStrategyConfig.builder()
            .onFailure(OnFailureConfig.builder()
                           .errors(Collections.singletonList(sameErrorForStepStageAndStepGroup))
                           .action(MarkAsSuccessFailureActionConfig.builder().build())
                           .build())
            .build());
    stepGroupFailureStrategies.add(FailureStrategyConfig.builder()
                                       .onFailure(OnFailureConfig.builder()
                                                      .errors(Collections.singletonList(sameErrorForStageAndStepGroup))
                                                      .action(MarkAsSuccessFailureActionConfig.builder().build())
                                                      .build())
                                       .build());

    Map<FailureStrategyActionConfig, Collection<FailureType>> actionMap =
        FailureStrategiesUtils.priorityMergeFailureStrategies(
            stepFailureStrategies, stepGroupFailureStrategies, stageFailureStrategies);

    assertThat(actionMap.get(ExpectedStepAction).contains(FailureType.AUTHENTICATION_FAILURE)).isEqualTo(true);
    assertThat(actionMap.get(ExpectedStepStageAction).contains(FailureType.AUTHORIZATION_FAILURE)).isEqualTo(true);
    assertThat(actionMap.get(ExpectedStageAction).contains(FailureType.TIMEOUT_FAILURE)).isEqualTo(true);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testpriorityDifferentErrorsDifferentAction() {
    NGFailureType sameErrorForStepStageAndStepGroup = AUTHENTICATION_ERROR;
    NGFailureType sameErrorForStageAndStepGroup = AUTHORIZATION_ERROR;
    NGFailureType ErrorOnlyForStage = TIMEOUT_ERROR;

    IgnoreFailureActionConfig ignoreAction = IgnoreFailureActionConfig.builder().build();
    MarkAsSuccessFailureActionConfig markAsSuccessAction = MarkAsSuccessFailureActionConfig.builder().build();
    AbortFailureActionConfig abortAction = AbortFailureActionConfig.builder().build();

    List<FailureStrategyConfig> stageFailureStrategies = new ArrayList<>();
    stageFailureStrategies.add(FailureStrategyConfig.builder()
                                   .onFailure(OnFailureConfig.builder()
                                                  .errors(Collections.singletonList(AUTHENTICATION_ERROR))
                                                  .action(AbortFailureActionConfig.builder().build())
                                                  .build())
                                   .build());
    stageFailureStrategies.add(FailureStrategyConfig.builder()
                                   .onFailure(OnFailureConfig.builder()
                                                  .errors(Collections.singletonList(CONNECTIVITY_ERROR))
                                                  .action(AbortFailureActionConfig.builder().build())
                                                  .build())
                                   .build());

    List<FailureStrategyConfig> stepFailureStrategies = new ArrayList<>();
    stepFailureStrategies.add(FailureStrategyConfig.builder()
                                  .onFailure(OnFailureConfig.builder()
                                                 .errors(Collections.singletonList(TIMEOUT_ERROR))
                                                 .action(IgnoreFailureActionConfig.builder().build())
                                                 .build())
                                  .build());
    stepFailureStrategies.add(FailureStrategyConfig.builder()
                                  .onFailure(OnFailureConfig.builder()
                                                 .errors(Collections.singletonList(AUTHORIZATION_ERROR))
                                                 .action(IgnoreFailureActionConfig.builder().build())
                                                 .build())
                                  .build());

    List<FailureStrategyConfig> stepGroupFailureStrategies = new ArrayList<>();
    stepGroupFailureStrategies.add(FailureStrategyConfig.builder()
                                       .onFailure(OnFailureConfig.builder()
                                                      .errors(Collections.singletonList(VERIFICATION_ERROR))
                                                      .action(MarkAsSuccessFailureActionConfig.builder().build())
                                                      .build())
                                       .build());

    Map<FailureStrategyActionConfig, Collection<FailureType>> actionMap =
        FailureStrategiesUtils.priorityMergeFailureStrategies(
            stepFailureStrategies, stepGroupFailureStrategies, stageFailureStrategies);

    assertThat(actionMap.get(abortAction).contains(FailureType.AUTHENTICATION_FAILURE)).isEqualTo(true);
    assertThat(actionMap.get(abortAction).contains(FailureType.CONNECTIVITY_FAILURE)).isEqualTo(true);
    assertThat(actionMap.get(markAsSuccessAction).contains(FailureType.VERIFICATION_FAILURE)).isEqualTo(true);
    assertThat(actionMap.get(ignoreAction).contains(FailureType.AUTHORIZATION_FAILURE)).isEqualTo(true);
    assertThat(actionMap.get(ignoreAction).contains(FailureType.TIMEOUT_FAILURE)).isEqualTo(true);
  }
}