package io.harness.functional.redesign.engine;

import static io.harness.execution.status.Status.FAILED;
import static io.harness.execution.status.Status.SUCCEEDED;
import static io.harness.rule.OwnerRule.ALEXEI;
import static io.harness.rule.OwnerRule.GARVIT;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.FunctionalTests;
import io.harness.execution.NodeExecution;
import io.harness.execution.PlanExecution;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.ApplicationGenerator.Applications;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.interrupts.ExecutionInterruptType;
import io.harness.interrupts.Interrupt;
import io.harness.redesign.states.http.BasicHttpStep;
import io.harness.redesign.states.shell.ShellScriptStepParameters;
import io.harness.rule.Owner;
import io.harness.steps.section.chain.SectionChainStep;
import io.harness.testframework.framework.MockServerExecutor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;

import java.util.List;
import java.util.stream.Collectors;

public class EngineFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
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

  @After
  public void shutDown() {
    mockServerExecutor.shutdownMockServer();
  }

  @Test
  @Owner(developers = ALEXEI, intermittent = true)
  @Category(FunctionalTests.class)
  public void shouldExecuteSwitchPlan() {
    PlanExecution httpSwitchResponse =
        executePlan(bearerToken, application.getAccountId(), application.getAppId(), "http-switch");

    assertThat(httpSwitchResponse.getStatus()).isEqualTo(SUCCEEDED);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(FunctionalTests.class)
  public void shouldExecuteForkPlan() {
    PlanExecution httpForkResponse =
        executePlan(bearerToken, application.getAccountId(), application.getAppId(), "http-fork");

    assertThat(httpForkResponse.getStatus()).isEqualTo(SUCCEEDED);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(FunctionalTests.class)
  public void shouldExecuteSectionPlan() {
    PlanExecution httpForkResponse =
        executePlan(bearerToken, application.getAccountId(), application.getAppId(), "http-section");

    assertThat(httpForkResponse.getStatus()).isEqualTo(SUCCEEDED);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(FunctionalTests.class)
  public void shouldExecuteSectionChainPlan() {
    PlanExecution httpForkResponse =
        executePlan(bearerToken, application.getAccountId(), application.getAppId(), "section-chain");

    assertThat(httpForkResponse.getStatus()).isEqualTo(SUCCEEDED);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(FunctionalTests.class)
  public void shouldExecuteSectionChainPlanWithFailure() {
    PlanExecution sectionFailureResponse =
        executePlan(bearerToken, application.getAccountId(), application.getAppId(), "section-chain-failure");

    List<NodeExecution> nodeExecutions = getNodeExecutions(sectionFailureResponse.getUuid());
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
    PlanExecution httpForkResponse =
        executePlan(bearerToken, application.getAccountId(), application.getAppId(), "section-chain-rollback");

    assertThat(httpForkResponse.getStatus()).isEqualTo(FAILED);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(FunctionalTests.class)
  public void shouldExecuteHttpRetryIgnorePlan() {
    PlanExecution httpRetryResponse =
        executePlan(bearerToken, application.getAccountId(), application.getAppId(), "http-retry-ignore");

    assertThat(httpRetryResponse.getStatus()).isEqualTo(FAILED);
    List<NodeExecution> nodeExecutions = getNodeExecutions(httpRetryResponse.getUuid());
    assertThat(nodeExecutions).hasSize(4);

    List<NodeExecution> retriedNodeExecutions =
        nodeExecutions.stream()
            .filter(ex -> ex.getNode().getStepType().equals(BasicHttpStep.STEP_TYPE))
            .collect(Collectors.toList());

    assertThat(retriedNodeExecutions).hasSize(3);

    // Pick Latest created one
    assertThat(retriedNodeExecutions.get(0).getRetryIds())
        .containsExactlyInAnyOrder(retriedNodeExecutions.get(1).getUuid(), retriedNodeExecutions.get(2).getUuid());

    List<Interrupt> interrupts = getPlanInterrupts(httpRetryResponse.getUuid());
    assertThat(interrupts).hasSize(2);
    assertThat(interrupts.stream().map(Interrupt::getType).collect(Collectors.toList()))
        .containsExactly(ExecutionInterruptType.RETRY, ExecutionInterruptType.RETRY);
  }

  @Test
  @Owner(developers = PRASHANT, intermittent = true)
  @Category(FunctionalTests.class)
  public void shouldExecuteHttpRetryAbortPlan() {
    PlanExecution httpRetryResponse =
        executePlan(bearerToken, application.getAccountId(), application.getAppId(), "http-retry-abort");

    assertThat(httpRetryResponse.getStatus()).isEqualTo(FAILED);
    List<NodeExecution> nodeExecutions = getNodeExecutions(httpRetryResponse.getUuid());
    assertThat(nodeExecutions).hasSize(3);

    List<NodeExecution> retriedNodeExecutions =
        nodeExecutions.stream()
            .filter(ex -> ex.getNode().getStepType().equals(BasicHttpStep.STEP_TYPE))
            .collect(Collectors.toList());

    assertThat(retriedNodeExecutions).hasSize(3);

    // Pick Latest created one
    assertThat(retriedNodeExecutions.get(0).getRetryIds())
        .containsExactlyInAnyOrder(retriedNodeExecutions.get(1).getUuid(), retriedNodeExecutions.get(2).getUuid());

    List<Interrupt> interrupts = getPlanInterrupts(httpRetryResponse.getUuid());
    assertThat(interrupts).hasSize(2);
    assertThat(interrupts.stream().map(Interrupt::getType).collect(Collectors.toList()))
        .containsExactly(ExecutionInterruptType.RETRY, ExecutionInterruptType.RETRY);
  }

  @Test
  @Owner(developers = GARVIT, intermittent = true)
  @Category(FunctionalTests.class)
  public void shouldExecuteSimpleShellScriptPlan() {
    PlanExecution shellScriptResponse =
        executePlan(bearerToken, application.getAccountId(), application.getAppId(), "simple-shell-script");

    assertThat(shellScriptResponse.getStatus()).isEqualTo(SUCCEEDED);

    List<NodeExecution> nodeExecutions = getNodeExecutions(shellScriptResponse.getUuid());
    assertThat(nodeExecutions).isNotNull();

    String shellScript11 = fetchShellScriptLogs(nodeExecutions, "shell11");
    String shellScript12 = fetchShellScriptLogs(nodeExecutions, "shell12");
    String shellScript2 = fetchShellScriptLogs(nodeExecutions, "shell2");

    String expectedShellScript11 = "echo 'Hello, world, from script 11!'\n"
        + "export HELLO='hello1!'\n"
        + "export HI='hi1!'\n"
        + "echo \"scriptType = BASH\"\n"
        + "echo \"sweepingOutputScope = SECTION\"\n";
    assertThat(shellScript11).isEqualTo(expectedShellScript11);

    String expectedShellScript12 = "echo 'Hello, world, from script 12!'\n"
        + "export HELLO='hello2!'\n"
        + "export HI='hi2!'\n"
        + "echo \"shell1.HELLO = hello1!\"\n"
        + "echo \"shell1.HI = hi1!\"\n"
        + "echo \"shell1.HELLO = hello1!\"\n"
        + "echo \"shell1.HI = hi1!\"\n"
        + "echo \"shell1.HELLO = hello1!\"\n"
        + "echo \"shell1.HI = hi1!\"\n"
        + "echo \"shell1.HELLO = hello1!\"\n"
        + "echo \"shell1.HI = hi1!\"\n"
        + "echo \"scriptType = BASH\"\n"
        + "echo \"section1.f1 = v11\"\n"
        + "echo \"section1.f2 = v12\"\n"
        + "echo \"sectionChild.f1 = v111\"\n"
        + "echo \"sectionChild.f1 = v111\"\n"
        + "echo \"sectionChild.f2 = v112\"\n"
        + "echo \"sectionChild.f2 = v112\"\n"
        + "echo \"shell1.HELLO = hello1!\"\n"
        + "echo \"shell1.HI = hi1!\"\n"
        + "echo \"shell1.HELLO = hello1!\"\n"
        + "echo \"shell1.HI = hi1!\"\n"
        + "echo \"shell1.HELLO = hello1!\"\n"
        + "echo \"shell1.HI = hi1!\"\n"
        + "echo \"shell1.HELLO = hello1!\"\n"
        + "echo \"shell1.HI = hi1!\"\n"
        + "echo \"scriptType = BASH\"\n"
        + "echo \"section1.f1 = v11\"\n"
        + "echo \"section1.f2 = v12\"\n"
        + "echo \"sectionChild.f1 = v111\"\n"
        + "echo \"sectionChild.f1 = v111\"\n"
        + "echo \"sectionChild.f2 = v112\"\n"
        + "echo \"sectionChild.f2 = v112\"\n";
    assertThat(shellScript12.trim()).isEqualTo(expectedShellScript12.trim());

    String expectedShellScript2 = "echo 'Hello, world, from script 2!'\n"
        + "echo \"shell1.HELLO = hello1!\"\n"
        + "echo \"shell1.HI = hi1!\"\n"
        + "echo \"shell1.HELLO = hello1!\"\n"
        + "echo \"shell1.HI = hi1!\"\n"
        + "echo \"shell2.HELLO = hello2!\"\n"
        + "echo \"shell2.HI = hi2!\"\n"
        + "echo \"section1.f1 = v11\"\n"
        + "echo \"section1.f1 = v11\"\n"
        + "echo \"section11.f1 = v111\"\n"
        + "echo \"section11.f1 = v111\"\n"
        + "echo \"shell2.scriptType = BASH\"\n"
        + "echo \"shell2.HELLO = hello2!\"\n"
        + "echo \"shell2.HI = hi2!\"\n"
        + "echo \"shell2.HELLO = hello2!\"\n"
        + "echo \"shell2.HI = hi2!\"\n"
        + "echo \"scriptType = BASH\"\n"
        + "echo \"section2.f1 = v21\"\n"
        + "echo \"section2.f2 = v22\"\n"
        + "echo \"sectionChild.f1 = v211\"\n"
        + "echo \"sectionChild.f1 = v211\"\n"
        + "echo \"sectionChild.f2 = v212\"\n"
        + "echo \"sectionChild.f2 = v212\"\n"
        + "echo \"shell1.HELLO = hello1!\"\n"
        + "echo \"shell1.HI = hi1!\"\n"
        + "echo \"shell1.HELLO = hello1!\"\n"
        + "echo \"shell1.HI = hi1!\"\n"
        + "echo \"shell2.HELLO = hello2!\"\n"
        + "echo \"shell2.HI = hi2!\"\n"
        + "echo \"section1.f1 = v11\"\n"
        + "echo \"section1.f1 = v11\"\n"
        + "echo \"section11.f1 = v111\"\n"
        + "echo \"section11.f1 = v111\"\n"
        + "echo \"shell2.scriptType = BASH\"\n"
        + "echo \"shell2.HELLO = hello2!\"\n"
        + "echo \"shell2.HI = hi2!\"\n"
        + "echo \"shell2.HELLO = hello2!\"\n"
        + "echo \"shell2.HI = hi2!\"\n"
        + "echo \"scriptType = BASH\"\n"
        + "echo \"section2.f1 = v21\"\n"
        + "echo \"section2.f2 = v22\"\n"
        + "echo \"sectionChild.f1 = v211\"\n"
        + "echo \"sectionChild.f1 = v211\"\n"
        + "echo \"sectionChild.f2 = v212\"\n"
        + "echo \"sectionChild.f2 = v212\"\n";
    assertThat(shellScript2).isEqualTo(expectedShellScript2);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(FunctionalTests.class)
  public void shouldExecuteMultipleBarriersPlan() {
    PlanExecution multipleBarriersResponse =
        executePlan(bearerToken, application.getAccountId(), application.getAppId(), "multiple-barriers");

    assertThat(multipleBarriersResponse.getStatus()).isEqualTo(SUCCEEDED);
  }

  private String fetchShellScriptLogs(List<NodeExecution> nodeExecutions, String name) {
    NodeExecution nodeExecution =
        nodeExecutions.stream().filter(ne -> name.equals(ne.getNode().getName())).findFirst().orElse(null);
    assertThat(nodeExecution).isNotNull();

    ShellScriptStepParameters shellScriptStepParameters =
        (ShellScriptStepParameters) nodeExecution.getResolvedStepParameters();
    assertThat(shellScriptStepParameters).isNotNull();
    return shellScriptStepParameters.getScriptString();
  }
}
