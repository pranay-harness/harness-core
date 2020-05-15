package io.harness.functional.redesign.engine;

import static io.harness.rule.OwnerRule.ALEXEI;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.FunctionalTests;
import io.harness.execution.PlanExecution;
import io.harness.execution.status.ExecutionInstanceStatus;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.ApplicationGenerator.Applications;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;

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

    assertThat(httpSwitchResponse.getStatus()).isEqualTo(ExecutionInstanceStatus.SUCCEEDED);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(FunctionalTests.class)
  public void shouldExecuteForkPlan() {
    PlanExecution httpForkResponse =
        executePlan(bearerToken, application.getAccountId(), application.getAppId(), "http-fork");

    assertThat(httpForkResponse.getStatus()).isEqualTo(ExecutionInstanceStatus.SUCCEEDED);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(FunctionalTests.class)
  public void shouldExecuteSectionPlan() {
    PlanExecution httpForkResponse =
        executePlan(bearerToken, application.getAccountId(), application.getAppId(), "http-section");

    assertThat(httpForkResponse.getStatus()).isEqualTo(ExecutionInstanceStatus.SUCCEEDED);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(FunctionalTests.class)
  @Ignore(value = "Remove ignore when Retry flow is fixed")
  public void shouldExecuteHttpRetryPlan() {
    PlanExecution httpForkResponse =
        executePlan(bearerToken, application.getAccountId(), application.getAppId(), "http-retry");

    assertThat(httpForkResponse.getStatus()).isEqualTo(ExecutionInstanceStatus.SUCCEEDED);
  }
}
