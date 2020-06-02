package io.harness.functional.redesign.engine;

import static io.harness.execution.status.Status.SUCCEEDED;
import static io.harness.rule.OwnerRule.ALEXEI;
import static io.harness.rule.OwnerRule.GARVIT;
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
import io.harness.redesign.states.shell.ShellScriptStepParameters;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;

import java.util.List;

public class ExecutionEngineTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;

  Owners owners;
  Application application;

  final Seed seed = new Seed(0);

  @Before
  public void setUp() {
    owners = ownerManager.create();
    application = applicationGenerator.ensurePredefined(seed, owners, Applications.GENERIC_TEST);
    assertThat(application).isNotNull();
  }

  @Test
  @Owner(developers = ALEXEI)
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
  @Owner(developers = ALEXEI)
  @Category(FunctionalTests.class)
  @Ignore(value = "Remove ignore when Retry flow is fixed")
  public void shouldExecuteHttpRetryPlan() {
    PlanExecution httpForkResponse =
        executePlan(bearerToken, application.getAccountId(), application.getAppId(), "http-retry");

    assertThat(httpForkResponse.getStatus()).isEqualTo(SUCCEEDED);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(FunctionalTests.class)
  public void shouldExecuteSimpleShellScriptPlan() {
    PlanExecution shellScriptResponse =
        executePlan(bearerToken, application.getAccountId(), application.getAppId(), "simple-shell-script");

    assertThat(shellScriptResponse.getStatus()).isEqualTo(SUCCEEDED);

    List<NodeExecution> nodeExecutions = getNodeExecutions(shellScriptResponse.getUuid());
    assertThat(nodeExecutions).isNotNull();

    String shellScript1 = fetchShellScriptLogs(nodeExecutions, "shell1");
    String expectedShellScript1 = "echo 'Hello, world, from script 1!'\n"
        + "export HELLO='hello!'\n"
        + "export HI='hi!'\n"
        + "echo \"scriptType = BASH\"\n"
        + "echo \"section1.f1 = v11\"\n"
        + "echo \"section1.f2 = v12\"\n"
        + "echo \"sectionChild.f1 = v111\"\n"
        + "echo \"sectionChild.f1 = v111\"\n"
        + "echo \"sectionChild.f2 = v112\"\n"
        + "echo \"sectionChild.f2 = v112\"\n"
        + "echo \"scriptType = BASH\"\n"
        + "echo \"section1.f1 = v11\"\n"
        + "echo \"section1.f2 = v12\"\n"
        + "echo \"sectionChild.f1 = v111\"\n"
        + "echo \"sectionChild.f1 = v111\"\n"
        + "echo \"sectionChild.f2 = v112\"\n"
        + "echo \"sectionChild.f2 = v112\"\n";
    assertThat(shellScript1.trim()).isEqualTo(expectedShellScript1.trim());

    String shellScript2 = fetchShellScriptLogs(nodeExecutions, "shell2");
    String expectedShellScript2 = "echo 'Hello, world, from script 2!'\n"
        + "echo \"shell1.HELLO = hello!\"\n"
        + "echo \"shell1.HI = hi!\"\n"
        + "echo \"section1.f1 = v11\"\n"
        + "echo \"section1.f1 = v11\"\n"
        + "echo \"section11.f1 = v111\"\n"
        + "echo \"section11.f1 = v111\"\n"
        + "echo \"shell1.scriptType = BASH\"\n"
        + "echo \"shell1.HELLO = hello!\"\n"
        + "echo \"shell1.HI = hi!\"\n"
        + "echo \"scriptType = BASH\"\n"
        + "echo \"section2.f1 = v21\"\n"
        + "echo \"section2.f2 = v22\"\n"
        + "echo \"sectionChild.f1 = v211\"\n"
        + "echo \"sectionChild.f1 = v211\"\n"
        + "echo \"sectionChild.f2 = v212\"\n"
        + "echo \"sectionChild.f2 = v212\"\n"
        + "echo \"shell1.HELLO = hello!\"\n"
        + "echo \"shell1.HI = hi!\"\n"
        + "echo \"section1.f1 = v11\"\n"
        + "echo \"section1.f1 = v11\"\n"
        + "echo \"section11.f1 = v111\"\n"
        + "echo \"section11.f1 = v111\"\n"
        + "echo \"shell1.scriptType = BASH\"\n"
        + "echo \"shell1.HELLO = hello!\"\n"
        + "echo \"shell1.HI = hi!\"\n"
        + "echo \"scriptType = BASH\"\n"
        + "echo \"section2.f1 = v21\"\n"
        + "echo \"section2.f2 = v22\"\n"
        + "echo \"sectionChild.f1 = v211\"\n"
        + "echo \"sectionChild.f1 = v211\"\n"
        + "echo \"sectionChild.f2 = v212\"\n"
        + "echo \"sectionChild.f2 = v212\"\n";
    assertThat(shellScript2).isEqualTo(expectedShellScript2);
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
