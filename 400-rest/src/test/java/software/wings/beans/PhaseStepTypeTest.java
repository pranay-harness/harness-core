package software.wings.beans;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import software.wings.WingsBaseTest;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class PhaseStepTypeTest extends WingsBaseTest {
  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void containsCustomDeploymentPhaseStep() {
    assertThat(PhaseStepType.valueOf("CUSTOM_DEPLOYMENT_PHASE_STEP")).isNotNull();
  }
}
