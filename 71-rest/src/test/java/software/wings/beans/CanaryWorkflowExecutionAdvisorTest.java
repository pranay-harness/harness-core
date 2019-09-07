package software.wings.beans;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.FailureType;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.EnumSet;
import java.util.List;

public class CanaryWorkflowExecutionAdvisorTest extends CategoryTest {
  @Test
  @Category(UnitTests.class)
  public void testSelectTopMatchingStrategyWithNulls() {
    assertThat(CanaryWorkflowExecutionAdvisor.selectTopMatchingStrategy(null, null, "dummy")).isNull();
  }

  @Test
  @Category(UnitTests.class)
  public void testSelectTopMatchingStrategyWithNullStrategy() {
    final List<FailureStrategy> failureStrategies = asList(
        FailureStrategy.builder().failureTypes(null).build(), FailureStrategy.builder().failureTypes(null).build());

    assertThat(CanaryWorkflowExecutionAdvisor.selectTopMatchingStrategy(failureStrategies, null, "dummy"))
        .isEqualTo(failureStrategies.get(0));

    assertThat(CanaryWorkflowExecutionAdvisor.selectTopMatchingStrategy(
                   failureStrategies, EnumSet.<FailureType>of(FailureType.APPLICATION_ERROR), "dummy"))
        .isEqualTo(failureStrategies.get(0));
  }

  @Test
  @Category(UnitTests.class)
  public void testSelectTopMatchingStrategyWithNullError() {
    final List<FailureStrategy> failureStrategies =
        asList(FailureStrategy.builder().failureTypes(asList(FailureType.APPLICATION_ERROR)).build(),
            FailureStrategy.builder().failureTypes(null).build());

    assertThat(CanaryWorkflowExecutionAdvisor.selectTopMatchingStrategy(failureStrategies, null, "dummy"))
        .isEqualTo(failureStrategies.get(0));

    assertThat(CanaryWorkflowExecutionAdvisor.selectTopMatchingStrategy(
                   failureStrategies, EnumSet.noneOf(FailureType.class), "dummy"))
        .isEqualTo(failureStrategies.get(0));
  }

  @Test
  @Category(UnitTests.class)
  public void testSelectTopMatchingStrategyFindMatching() {
    final List<FailureStrategy> failureStrategies =
        asList(FailureStrategy.builder().failureTypes(asList(FailureType.APPLICATION_ERROR)).build(),
            FailureStrategy.builder().failureTypes(asList(FailureType.CONNECTIVITY)).build());

    assertThat(CanaryWorkflowExecutionAdvisor.selectTopMatchingStrategy(
                   failureStrategies, EnumSet.<FailureType>of(FailureType.CONNECTIVITY), "dummy"))
        .isEqualTo(failureStrategies.get(1));

    assertThat(CanaryWorkflowExecutionAdvisor.selectTopMatchingStrategy(
                   failureStrategies, EnumSet.<FailureType>of(FailureType.VERIFICATION_FAILURE), "dummy"))
        .isNull();
  }

  @Test
  @Category(UnitTests.class)
  public void testSelectTopMatchingStepWithNull() {
    final List<FailureStrategy> failureStrategies =
        asList(FailureStrategy.builder().specificSteps(null).build(), FailureStrategy.builder().build());

    assertThat(CanaryWorkflowExecutionAdvisor.selectTopMatchingStrategy(failureStrategies, null, "dummy"))
        .isEqualTo(failureStrategies.get(0));
  }

  @Test
  @Category(UnitTests.class)
  public void testSelectTopMatchingStepWithNone() {
    final List<FailureStrategy> failureStrategies =
        asList(FailureStrategy.builder().specificSteps(asList("n/a")).build());

    assertThat(CanaryWorkflowExecutionAdvisor.selectTopMatchingStrategy(failureStrategies, null, "dummy")).isNull();
  }

  @Test
  @Category(UnitTests.class)
  public void testSelectTopMatchingStep() {
    final List<FailureStrategy> failureStrategies =
        asList(FailureStrategy.builder().specificSteps(asList("dummy")).build());

    assertThat(CanaryWorkflowExecutionAdvisor.selectTopMatchingStrategy(failureStrategies, null, "dummy"))
        .isEqualTo(failureStrategies.get(0));
  }

  @Test
  @Category(UnitTests.class)
  public void testSelectTopNonMatchingCombo() {
    final List<FailureStrategy> failureStrategies = asList(
        FailureStrategy.builder().specificSteps(asList("n/a")).build(),
        FailureStrategy.builder().failureTypes(asList(FailureType.APPLICATION_ERROR)).build(),
        FailureStrategy.builder()
            .specificSteps(asList("dummy"))
            .failureTypes(asList(FailureType.APPLICATION_ERROR))
            .build(),
        FailureStrategy.builder().specificSteps(asList("n/a")).failureTypes(asList(FailureType.CONNECTIVITY)).build());

    assertThat(CanaryWorkflowExecutionAdvisor.selectTopMatchingStrategy(
                   failureStrategies, EnumSet.<FailureType>of(FailureType.CONNECTIVITY), "dummy"))
        .isNull();
  }

  @Test
  @Category(UnitTests.class)
  public void testSelectTopMatchingCombo() {
    final List<FailureStrategy> failureStrategies = asList(
        FailureStrategy.builder().specificSteps(asList("n/a")).build(),
        FailureStrategy.builder().failureTypes(asList(FailureType.APPLICATION_ERROR)).build(),
        FailureStrategy.builder()
            .specificSteps(asList("dummy"))
            .failureTypes(asList(FailureType.APPLICATION_ERROR))
            .build(),
        FailureStrategy.builder().specificSteps(asList("n/a")).failureTypes(asList(FailureType.CONNECTIVITY)).build(),
        FailureStrategy.builder()
            .specificSteps(asList("dummy"))
            .failureTypes(asList(FailureType.CONNECTIVITY))
            .build());

    assertThat(CanaryWorkflowExecutionAdvisor.selectTopMatchingStrategy(
                   failureStrategies, EnumSet.<FailureType>of(FailureType.CONNECTIVITY), "dummy"))
        .isEqualTo(failureStrategies.get(4));
  }
}