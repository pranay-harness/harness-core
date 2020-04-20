package software.wings.sm.states.k8s;

import static io.harness.rule.OwnerRule.YOGESH;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

public class K8sRollingDeployRollbackTest extends WingsBaseTest {
  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testTimeoutValue() {
    K8sRollingDeployRollback state = new K8sRollingDeployRollback("k8s-rollback");
    assertThat(state.getTimeoutMillis()).isNull();

    state.setStateTimeoutInMinutes(5);
    assertThat(state.getTimeoutMillis()).isEqualTo(300000);
  }
}