package software.wings.beans;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

public class PhaseStepTypeTest extends WingsBaseTest {
  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void containsCustomDeploymentPhaseStep() {
    Assertions.assertThat(PhaseStepType.valueOf("CUSTOM_DEPLOYMENT_PHASE_STEP")).isNotNull();
  }
}
