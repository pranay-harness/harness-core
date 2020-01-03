package software.wings.beans;

import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.exception.FailureType;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.WorkflowStandardParams.Builder;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

public class CanaryWorkflowExecutionAdvisorTest extends CategoryTest {
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testSelectTopMatchingStrategyWithNulls() {
    assertThat(CanaryWorkflowExecutionAdvisor.selectTopMatchingStrategy(null, null, "dummy")).isNull();
  }

  @Test
  @Owner(developers = GEORGE)
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
  @Owner(developers = GEORGE)
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
  @Owner(developers = GEORGE)
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
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testSelectTopMatchingStepWithNull() {
    final List<FailureStrategy> failureStrategies =
        asList(FailureStrategy.builder().specificSteps(null).build(), FailureStrategy.builder().build());

    assertThat(CanaryWorkflowExecutionAdvisor.selectTopMatchingStrategy(failureStrategies, null, "dummy"))
        .isEqualTo(failureStrategies.get(0));
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testSelectTopMatchingStepWithNone() {
    final List<FailureStrategy> failureStrategies =
        asList(FailureStrategy.builder().specificSteps(asList("n/a")).build());

    assertThat(CanaryWorkflowExecutionAdvisor.selectTopMatchingStrategy(failureStrategies, null, "dummy")).isNull();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testSelectTopMatchingStep() {
    final List<FailureStrategy> failureStrategies =
        asList(FailureStrategy.builder().specificSteps(asList("dummy")).build());

    assertThat(CanaryWorkflowExecutionAdvisor.selectTopMatchingStrategy(failureStrategies, null, "dummy"))
        .isEqualTo(failureStrategies.get(0));
  }

  @Test
  @Owner(developers = GEORGE)
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
  @Owner(developers = GEORGE)
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

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldReturnTrueWhenExecutionHostsPresent() {
    CanaryWorkflowExecutionAdvisor canaryWorkflowExecutionAdvisor = new CanaryWorkflowExecutionAdvisor();
    ExecutionContextImpl context = mock(ExecutionContextImpl.class);
    WorkflowStandardParams workflowStandardParams =
        Builder.aWorkflowStandardParams().withExecutionHosts(Collections.singletonList("host1")).build();
    doReturn(workflowStandardParams).when(context).getContextElement(ContextElementType.STANDARD);

    boolean executionHostsPresent = canaryWorkflowExecutionAdvisor.isExecutionHostsPresent(context);

    assertThat(executionHostsPresent).isTrue();
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldReturnFalseWhenExecutionHostsNotPresent() {
    CanaryWorkflowExecutionAdvisor canaryWorkflowExecutionAdvisor = new CanaryWorkflowExecutionAdvisor();
    ExecutionContextImpl context = mock(ExecutionContextImpl.class);
    WorkflowStandardParams workflowStandardParams = Builder.aWorkflowStandardParams().withExecutionHosts(null).build();
    doReturn(workflowStandardParams).when(context).getContextElement(ContextElementType.STANDARD);

    boolean executionHostsPresent = canaryWorkflowExecutionAdvisor.isExecutionHostsPresent(context);

    assertThat(executionHostsPresent).isFalse();
  }
}