package software.wings.sm;

import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.api.DeploymentType.AMI;
import static software.wings.api.DeploymentType.ECS;
import static software.wings.api.DeploymentType.PCF;
import static software.wings.api.DeploymentType.SPOTINST;
import static software.wings.sm.StepType.AWS_AMI_SERVICE_SETUP;

import io.harness.CategoryTest;
import io.harness.beans.OrchestrationWorkflowType;
import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class StepTypeTest extends CategoryTest {
  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldMatchForECS() {
    StepType stepType = StepType.ECS_SERVICE_SETUP;
    assertThat(stepType.matches(ECS, OrchestrationWorkflowType.BASIC)).isTrue();
    assertThat(stepType.matches(ECS, OrchestrationWorkflowType.CANARY)).isTrue();
    assertThat(stepType.matches(ECS, OrchestrationWorkflowType.BLUE_GREEN)).isFalse();

    stepType = StepType.ECS_DAEMON_SERVICE_SETUP;
    assertThat(stepType.matches(ECS, OrchestrationWorkflowType.BASIC)).isTrue();
    assertThat(stepType.matches(ECS, OrchestrationWorkflowType.CANARY)).isFalse();
    assertThat(stepType.matches(ECS, OrchestrationWorkflowType.BLUE_GREEN)).isFalse();

    stepType = StepType.ECS_SERVICE_DEPLOY;
    assertThat(stepType.matches(ECS, OrchestrationWorkflowType.BASIC)).isTrue();
    assertThat(stepType.matches(ECS, OrchestrationWorkflowType.CANARY)).isTrue();
    assertThat(stepType.matches(ECS, OrchestrationWorkflowType.BLUE_GREEN)).isTrue();

    stepType = StepType.ECS_SERVICE_ROLLBACK;
    assertThat(stepType.matches(ECS, OrchestrationWorkflowType.BASIC)).isTrue();
    assertThat(stepType.matches(ECS, OrchestrationWorkflowType.CANARY)).isTrue();
    assertThat(stepType.matches(ECS, OrchestrationWorkflowType.BLUE_GREEN)).isTrue();

    stepType = StepType.ECS_BG_SERVICE_SETUP;
    assertThat(stepType.matches(ECS, OrchestrationWorkflowType.BASIC)).isFalse();
    assertThat(stepType.matches(ECS, OrchestrationWorkflowType.CANARY)).isFalse();
    assertThat(stepType.matches(ECS, OrchestrationWorkflowType.BLUE_GREEN)).isTrue();

    stepType = StepType.ECS_BG_SERVICE_SETUP_ROUTE53;
    assertThat(stepType.matches(ECS, OrchestrationWorkflowType.BASIC)).isFalse();
    assertThat(stepType.matches(ECS, OrchestrationWorkflowType.CANARY)).isFalse();
    assertThat(stepType.matches(ECS, OrchestrationWorkflowType.BLUE_GREEN)).isTrue();

    stepType = StepType.ECS_LISTENER_UPDATE;
    assertThat(stepType.matches(ECS, OrchestrationWorkflowType.BASIC)).isFalse();
    assertThat(stepType.matches(ECS, OrchestrationWorkflowType.CANARY)).isFalse();
    assertThat(stepType.matches(ECS, OrchestrationWorkflowType.BLUE_GREEN)).isTrue();

    stepType = StepType.ECS_LISTENER_UPDATE_ROLLBACK;
    assertThat(stepType.matches(ECS, OrchestrationWorkflowType.BASIC)).isFalse();
    assertThat(stepType.matches(ECS, OrchestrationWorkflowType.CANARY)).isFalse();
    assertThat(stepType.matches(ECS, OrchestrationWorkflowType.BLUE_GREEN)).isTrue();

    stepType = StepType.ECS_ROUTE53_DNS_WEIGHT_UPDATE;
    assertThat(stepType.matches(ECS, OrchestrationWorkflowType.BASIC)).isFalse();
    assertThat(stepType.matches(ECS, OrchestrationWorkflowType.CANARY)).isFalse();
    assertThat(stepType.matches(ECS, OrchestrationWorkflowType.BLUE_GREEN)).isTrue();

    stepType = StepType.ECS_ROUTE53_DNS_WEIGHT_UPDATE_ROLLBACK;
    assertThat(stepType.matches(ECS, OrchestrationWorkflowType.BASIC)).isFalse();
    assertThat(stepType.matches(ECS, OrchestrationWorkflowType.CANARY)).isFalse();
    assertThat(stepType.matches(ECS, OrchestrationWorkflowType.BLUE_GREEN)).isTrue();

    // Negative Test
    stepType = StepType.ECS_SERVICE_DEPLOY;
    assertThat(stepType.matches(SPOTINST, OrchestrationWorkflowType.BASIC)).isFalse();
    assertThat(stepType.matches(SPOTINST, OrchestrationWorkflowType.CANARY)).isFalse();
    assertThat(stepType.matches(SPOTINST, OrchestrationWorkflowType.BLUE_GREEN)).isFalse();
  }

  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldMatchForSpotinst() {
    StepType stepType = StepType.SPOTINST_SETUP;
    assertThat(stepType.matches(AMI, OrchestrationWorkflowType.BASIC)).isTrue();
    assertThat(stepType.matches(AMI, OrchestrationWorkflowType.CANARY)).isTrue();
    assertThat(stepType.matches(AMI, OrchestrationWorkflowType.BLUE_GREEN)).isTrue();

    stepType = StepType.SPOTINST_DEPLOY;
    assertThat(stepType.matches(AMI, OrchestrationWorkflowType.BASIC)).isTrue();
    assertThat(stepType.matches(AMI, OrchestrationWorkflowType.CANARY)).isTrue();
    assertThat(stepType.matches(AMI, OrchestrationWorkflowType.BLUE_GREEN)).isTrue();

    stepType = StepType.SPOTINST_ROLLBACK;
    assertThat(stepType.matches(AMI, OrchestrationWorkflowType.BASIC)).isTrue();
    assertThat(stepType.matches(AMI, OrchestrationWorkflowType.CANARY)).isTrue();
    assertThat(stepType.matches(AMI, OrchestrationWorkflowType.BLUE_GREEN)).isTrue();

    stepType = StepType.SPOTINST_LISTENER_UPDATE;
    assertThat(stepType.matches(AMI, OrchestrationWorkflowType.BASIC)).isFalse();
    assertThat(stepType.matches(AMI, OrchestrationWorkflowType.CANARY)).isFalse();
    assertThat(stepType.matches(AMI, OrchestrationWorkflowType.BLUE_GREEN)).isTrue();

    stepType = StepType.SPOTINST_LISTENER_UPDATE_ROLLBACK;
    assertThat(stepType.matches(AMI, OrchestrationWorkflowType.BASIC)).isFalse();
    assertThat(stepType.matches(AMI, OrchestrationWorkflowType.CANARY)).isFalse();
    assertThat(stepType.matches(AMI, OrchestrationWorkflowType.BLUE_GREEN)).isTrue();
  }

  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldMatchForPcf() {
    StepType stepType = StepType.PCF_SETUP;
    assertThat(stepType.matches(PCF, OrchestrationWorkflowType.BASIC)).isTrue();
    assertThat(stepType.matches(PCF, OrchestrationWorkflowType.CANARY)).isTrue();
    assertThat(stepType.matches(PCF, OrchestrationWorkflowType.BLUE_GREEN)).isTrue();

    stepType = StepType.PCF_RESIZE;
    assertThat(stepType.matches(PCF, OrchestrationWorkflowType.BASIC)).isTrue();
    assertThat(stepType.matches(PCF, OrchestrationWorkflowType.CANARY)).isTrue();
    assertThat(stepType.matches(PCF, OrchestrationWorkflowType.BLUE_GREEN)).isTrue();

    stepType = StepType.PCF_BG_MAP_ROUTE;
    assertThat(stepType.matches(PCF, OrchestrationWorkflowType.BASIC)).isFalse();
    assertThat(stepType.matches(PCF, OrchestrationWorkflowType.CANARY)).isFalse();
    assertThat(stepType.matches(PCF, OrchestrationWorkflowType.BLUE_GREEN)).isTrue();

    stepType = StepType.PCF_ROLLBACK;
    assertThat(stepType.matches(PCF, OrchestrationWorkflowType.BASIC)).isTrue();
    assertThat(stepType.matches(PCF, OrchestrationWorkflowType.CANARY)).isTrue();
    assertThat(stepType.matches(PCF, OrchestrationWorkflowType.BLUE_GREEN)).isTrue();
  }

  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldMatchForOnesNotAddingTypeYet() {
    StepType stepType = AWS_AMI_SERVICE_SETUP;
    assertThat(stepType.matches(AMI, OrchestrationWorkflowType.BASIC)).isTrue();
    assertThat(stepType.matches(AMI, OrchestrationWorkflowType.CANARY)).isTrue();
    assertThat(stepType.matches(AMI, OrchestrationWorkflowType.BLUE_GREEN)).isTrue();
  }
}
