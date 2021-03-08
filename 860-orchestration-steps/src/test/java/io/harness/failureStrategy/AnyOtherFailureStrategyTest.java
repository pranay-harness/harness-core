package io.harness.failureStrategy;

import static io.harness.rule.OwnerRule.PRASHANTSHARMA;
import static io.harness.yaml.core.failurestrategy.NGFailureType.ANY_OTHER_ERRORS;
import static io.harness.yaml.core.failurestrategy.NGFailureType.AUTHORIZATION_ERROR;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.plancreator.steps.GenericStepPMSPlanCreator;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.yaml.core.failurestrategy.FailureStrategyConfig;
import io.harness.yaml.core.failurestrategy.NGFailureType;
import io.harness.yaml.core.failurestrategy.OnFailureConfig;
import io.harness.yaml.core.failurestrategy.abort.AbortFailureActionConfig;
import io.harness.yaml.core.failurestrategy.retry.RetryFailureActionConfig;
import io.harness.yaml.core.failurestrategy.retry.RetryFailureSpecConfig;
import io.harness.yaml.core.timeout.Timeout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class AnyOtherFailureStrategyTest extends CategoryTest {
  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testAtleastAnyOtherFailureStrategyExists() {
    List<FailureStrategyConfig> stageFailureStrategies1, stageFailureStrategies2, stageFailureStrategies3;
    // Not containing error type as ANY_OTHER_ERRORS
    stageFailureStrategies1 = Collections.singletonList(
        FailureStrategyConfig.builder()
            .onFailure(OnFailureConfig.builder()
                           .errors(Collections.singletonList(AUTHORIZATION_ERROR))
                           .action(RetryFailureActionConfig.builder()
                                       .specConfig(RetryFailureSpecConfig.builder()
                                                       .retryCount(ParameterField.createValueField(4))
                                                       .retryIntervals(ParameterField.createValueField(
                                                           Collections.singletonList(Timeout.fromString("2s"))))
                                                       .build())
                                       .build())
                           .build())
            .build());

    boolean ans = new GenericStepPMSPlanCreator() {
      @Override
      public Set<String> getSupportedStepTypes() {
        return null;
      }
    }.containsOnlyAnyOtherError(stageFailureStrategies1);
    assertThat(ans).isEqualTo(false);

    // Containing error type as ANY_OTHER_ERRORS only
    stageFailureStrategies2 =
        Collections.singletonList(FailureStrategyConfig.builder()
                                      .onFailure(OnFailureConfig.builder()
                                                     .errors(Collections.singletonList(ANY_OTHER_ERRORS))
                                                     .action(AbortFailureActionConfig.builder().build())
                                                     .build())
                                      .build());

    ans = new GenericStepPMSPlanCreator() {
      @Override
      public Set<String> getSupportedStepTypes() {
        return null;
      }
    }.containsOnlyAnyOtherError(stageFailureStrategies2);
    assertThat(ans).isEqualTo(true);

    // Containing other error along with ANY_OTHER_ERRORS
    List<NGFailureType> test = new ArrayList<>();
    test.add(AUTHORIZATION_ERROR);
    test.add(ANY_OTHER_ERRORS);
    stageFailureStrategies3 = Collections.singletonList(
        FailureStrategyConfig.builder()
            .onFailure(
                OnFailureConfig.builder().errors(test).action(AbortFailureActionConfig.builder().build()).build())
            .build());
    ans = new GenericStepPMSPlanCreator() {
      @Override
      public Set<String> getSupportedStepTypes() {
        return null;
      }
    }.containsOnlyAnyOtherError(stageFailureStrategies3);
    assertThat(ans).isEqualTo(false);
  }
}
