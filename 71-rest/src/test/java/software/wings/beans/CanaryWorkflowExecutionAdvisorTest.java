package software.wings.beans;

import static io.harness.rule.OwnerRule.GARVIT;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.exception.FailureType;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.api.PhaseElement;
import software.wings.beans.workflow.StepSkipStrategy;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionEventAdvice;
import software.wings.sm.State;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.WorkflowStandardParams.Builder;
import software.wings.sm.states.ShellScriptState;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
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

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testFindPhaseStep() {
    State state = new ShellScriptState("script-tmp");

    // Args are invalid/null.
    assertThat(CanaryWorkflowExecutionAdvisor.findPhaseStep(null, null, null)).isNull();
    assertThat(CanaryWorkflowExecutionAdvisor.findPhaseStep(aCanaryOrchestrationWorkflow().build(), null, state))
        .isNull();

    // State parent id is null.
    assertThat(CanaryWorkflowExecutionAdvisor.findPhaseStep(
                   aCanaryOrchestrationWorkflow().build(), PhaseElement.builder().build(), state))
        .isNull();

    // Unknown phase.
    String parentId = "parentId";
    state.setParentId(parentId);
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        aCanaryOrchestrationWorkflow()
            .withPreDeploymentSteps(aPhaseStep(PhaseStepType.PRE_DEPLOYMENT).build())
            .withPostDeploymentSteps(aPhaseStep(PhaseStepType.POST_DEPLOYMENT).build())
            .withWorkflowPhaseIdMap(new HashMap<>())
            .build();
    PhaseElement phaseElement = PhaseElement.builder().uuid(parentId).build();
    assertThat(CanaryWorkflowExecutionAdvisor.findPhaseStep(orchestrationWorkflow, phaseElement, state)).isNull();

    // Phase doesn't contains any phase step.
    WorkflowPhase phase = aWorkflowPhase().build();
    orchestrationWorkflow.setWorkflowPhaseIdMap(Collections.singletonMap(parentId, phase));
    assertThat(CanaryWorkflowExecutionAdvisor.findPhaseStep(orchestrationWorkflow, phaseElement, state)).isNull();

    // Phase doesn't contain required phase step.
    PhaseStep phaseStep = aPhaseStep(PhaseStepType.ENABLE_SERVICE).build();
    phaseStep.setUuid(parentId + "_tmp");
    phase.setPhaseSteps(Collections.singletonList(phaseStep));
    assertThat(CanaryWorkflowExecutionAdvisor.findPhaseStep(orchestrationWorkflow, phaseElement, state)).isNull();

    phaseStep.setUuid(parentId);
    phase.setPhaseSteps(Collections.singletonList(phaseStep));
    assertThat(CanaryWorkflowExecutionAdvisor.findPhaseStep(orchestrationWorkflow, phaseElement, state))
        .isEqualTo(phaseStep);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testShouldSkipStep() {
    // Args are invalid/null.
    assertThat(CanaryWorkflowExecutionAdvisor.shouldSkipStep(null, null, null)).isNull();
    assertThat(
        CanaryWorkflowExecutionAdvisor.shouldSkipStep(null, aPhaseStep(PhaseStepType.ENABLE_SERVICE).build(), null))
        .isNull();

    String expr = "${expr.name} == \"qa\"";
    String stateId = "stateId";
    State state = new ShellScriptState("script-tmp");
    state.setId(stateId);

    // No step skip strategy found for step id.
    PhaseStep phaseStep =
        aPhaseStep(PhaseStepType.ENABLE_SERVICE)
            .withStepSkipStrategies(Collections.singletonList(new StepSkipStrategy(
                StepSkipStrategy.Scope.SPECIFIC_STEPS, Collections.singletonList(stateId + "_tmp"), expr)))
            .build();
    assertThat(CanaryWorkflowExecutionAdvisor.shouldSkipStep(null, phaseStep, state)).isNull();

    ExecutionContextImpl context = spy(new ExecutionContextImpl(null));

    // Assertion expression evaluating to false.
    doReturn(false).when(context).evaluateExpression(any());
    assertThat(CanaryWorkflowExecutionAdvisor.shouldSkipStep(context, phaseStep, state)).isNull();

    // Assertion expression evaluating to true.
    phaseStep.setStepSkipStrategies(asList(
        new StepSkipStrategy(StepSkipStrategy.Scope.SPECIFIC_STEPS, Collections.singletonList(stateId + "_tmp"), expr),
        new StepSkipStrategy(StepSkipStrategy.Scope.SPECIFIC_STEPS, Collections.singletonList(stateId), expr)));
    doReturn(true).when(context).evaluateExpression(any());
    ExecutionEventAdvice advice = CanaryWorkflowExecutionAdvisor.shouldSkipStep(context, phaseStep, state);
    assertThat(advice).isNotNull();
    assertThat(advice.isSkipState()).isTrue();
    assertThat(advice.getSkipExpression()).isEqualTo(expr);
    assertThat(advice.getSkipError()).isNull();

    doThrow(new InvalidRequestException("error evaluating expression")).when(context).evaluateExpression(any());
    advice = CanaryWorkflowExecutionAdvisor.shouldSkipStep(context, phaseStep, state);
    assertThat(advice).isNotNull();
    assertThat(advice.isSkipState()).isTrue();
    assertThat(advice.getSkipExpression()).isEqualTo(expr);
    assertThat(advice.getSkipError()).isNotBlank();
  }
}