package io.harness.functional.redesign.engine;

import static io.harness.pms.contracts.execution.Status.ABORTED;
import static io.harness.pms.contracts.execution.Status.FAILED;
import static io.harness.pms.contracts.execution.Status.SUCCEEDED;
import static io.harness.rule.OwnerRule.ALEXEI;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.FunctionalTests;
import io.harness.execution.NodeExecution;
import io.harness.execution.PlanExecution;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.functional.redesign.OrchestrationEngineTestSetupHelper;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.ApplicationGenerator.Applications;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.interrupts.Interrupt;
import io.harness.pms.contracts.interrupts.InterruptType;
import io.harness.redesign.states.http.BasicHttpStep;
import io.harness.rule.Owner;
import io.harness.steps.section.chain.SectionChainStep;
import io.harness.testframework.framework.MockServerExecutor;

import software.wings.beans.Application;

import com.google.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class EngineFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private OrchestrationEngineTestSetupHelper testSetupHelper;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private MockServerExecutor mockServerExecutor;

  Owners owners;
  Application application;

  final Seed seed = new Seed(0);

  @Before
  public void setUp() {
    owners = ownerManager.create();
    application = applicationGenerator.ensurePredefined(seed, owners, Applications.GENERIC_TEST);
    assertThat(application).isNotNull();
    mockServerExecutor.ensureMockServer(AbstractFunctionalTest.class);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(FunctionalTests.class)
  public void shouldExecuteSwitchPlan() {
    PlanExecution httpSwitchResponse =
        testSetupHelper.executePlan(bearerToken, application.getAccountId(), application.getAppId(), "http-switch");

    assertThat(httpSwitchResponse.getStatus()).isEqualTo(SUCCEEDED);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(FunctionalTests.class)
  public void shouldExecuteForkPlan() {
    PlanExecution httpForkResponse =
        testSetupHelper.executePlan(bearerToken, application.getAccountId(), application.getAppId(), "http-fork");

    assertThat(httpForkResponse.getStatus()).isEqualTo(SUCCEEDED);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(FunctionalTests.class)
  public void shouldExecuteSectionPlan() {
    PlanExecution httpForkResponse =
        testSetupHelper.executePlan(bearerToken, application.getAccountId(), application.getAppId(), "http-section");

    assertThat(httpForkResponse.getStatus()).isEqualTo(SUCCEEDED);
  }

  @Test
  @Owner(developers = PRASHANT, intermittent = true)
  @Category(FunctionalTests.class)
  public void shouldExecuteSectionChainPlan() {
    PlanExecution httpForkResponse =
        testSetupHelper.executePlan(bearerToken, application.getAccountId(), application.getAppId(), "section-chain");

    assertThat(httpForkResponse.getStatus()).isEqualTo(SUCCEEDED);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(FunctionalTests.class)
  public void shouldExecuteSectionChainPlanWithFailure() {
    PlanExecution sectionFailureResponse = testSetupHelper.executePlan(
        bearerToken, application.getAccountId(), application.getAppId(), "section-chain-failure");

    List<NodeExecution> nodeExecutions = testSetupHelper.getNodeExecutions(sectionFailureResponse.getUuid());
    assertThat(nodeExecutions).hasSize(3);

    NodeExecution sectionChainExecution =
        nodeExecutions.stream()
            .filter(ex -> ex.getNode().getStepType().equals(SectionChainStep.STEP_TYPE))
            .findFirst()
            .orElse(null);

    assertThat(sectionChainExecution).isNotNull();
    assertThat(sectionChainExecution.getStatus()).isEqualTo(FAILED);
    assertThat(sectionFailureResponse.getStatus()).isEqualTo(FAILED);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(FunctionalTests.class)
  public void shouldExecuteSectionChainRollbackPlan() {
    PlanExecution httpForkResponse = testSetupHelper.executePlan(
        bearerToken, application.getAccountId(), application.getAppId(), "section-chain-rollback");

    assertThat(httpForkResponse.getStatus()).isEqualTo(FAILED);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(FunctionalTests.class)
  public void shouldExecuteHttpRetryIgnorePlan() {
    PlanExecution httpRetryResponse = testSetupHelper.executePlan(
        bearerToken, application.getAccountId(), application.getAppId(), "http-retry-ignore");

    assertThat(httpRetryResponse.getStatus()).isEqualTo(SUCCEEDED);
    List<NodeExecution> nodeExecutions = testSetupHelper.getNodeExecutions(httpRetryResponse.getUuid());
    assertThat(nodeExecutions).hasSize(4);

    List<NodeExecution> retriedNodeExecutions =
        nodeExecutions.stream()
            .filter(ex -> ex.getNode().getStepType().equals(BasicHttpStep.STEP_TYPE))
            .collect(Collectors.toList());

    assertThat(retriedNodeExecutions).hasSize(3);

    // Pick Latest created one
    assertThat(retriedNodeExecutions.get(0).getRetryIds())
        .containsExactlyInAnyOrder(retriedNodeExecutions.get(1).getUuid(), retriedNodeExecutions.get(2).getUuid());

    List<Interrupt> interrupts = testSetupHelper.getPlanInterrupts(httpRetryResponse.getUuid());
    assertThat(interrupts).hasSize(2);
    assertThat(interrupts.stream().map(Interrupt::getType).collect(Collectors.toList()))
        .containsExactly(InterruptType.RETRY, InterruptType.RETRY);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(FunctionalTests.class)
  @Ignore("Change in abort helper to handle if no node of finalizable status is found.")
  public void shouldExecuteHttpRetryAbortPlan() {
    PlanExecution httpRetryResponse = testSetupHelper.executePlan(
        bearerToken, application.getAccountId(), application.getAppId(), "http-retry-abort");

    assertThat(httpRetryResponse.getStatus()).isEqualTo(ABORTED);
    List<NodeExecution> nodeExecutions = testSetupHelper.getNodeExecutions(httpRetryResponse.getUuid());
    assertThat(nodeExecutions).hasSize(3);

    List<NodeExecution> retriedNodeExecutions =
        nodeExecutions.stream()
            .filter(ex -> ex.getNode().getStepType().equals(BasicHttpStep.STEP_TYPE))
            .collect(Collectors.toList());

    assertThat(retriedNodeExecutions).hasSize(3);

    // Pick Latest created one
    assertThat(retriedNodeExecutions.get(0).getRetryIds())
        .containsExactlyInAnyOrder(retriedNodeExecutions.get(1).getUuid(), retriedNodeExecutions.get(2).getUuid());

    List<Interrupt> interrupts = testSetupHelper.getPlanInterrupts(httpRetryResponse.getUuid());
    assertThat(interrupts).hasSize(2);
    assertThat(interrupts.stream().map(Interrupt::getType).collect(Collectors.toList()))
        .containsExactly(InterruptType.RETRY, InterruptType.RETRY);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(FunctionalTests.class)
  @Ignore("Ignore as barrier require yaml right now")
  public void shouldExecuteMultipleBarriersPlan() {
    PlanExecution multipleBarriersResponse = testSetupHelper.executePlan(
        bearerToken, application.getAccountId(), application.getAppId(), "multiple-barriers");

    assertThat(multipleBarriersResponse.getStatus()).isEqualTo(SUCCEEDED);
  }

  @Test
  @Owner(developers = ALEXEI, intermittent = true)
  @Category(FunctionalTests.class)
  @Ignore("Ingore while issues with setExpressionFunctorToken() will be resolved")
  public void shouldExecuteTaskChain() {
    PlanExecution taskChainResponse =
        testSetupHelper.executePlan(bearerToken, application.getAccountId(), application.getAppId(), "task-chain-v1");

    assertThat(taskChainResponse.getStatus()).isEqualTo(SUCCEEDED);
  }
}
