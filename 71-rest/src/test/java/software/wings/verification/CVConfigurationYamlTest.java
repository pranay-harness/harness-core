package software.wings.verification;

import static io.harness.rule.OwnerRule.SOWMYA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.wings.verification.CVConfiguration.CVConfigurationYaml;
import static software.wings.verification.prometheus.PrometheusCVServiceConfiguration.PrometheusCVConfigurationYaml;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

import java.util.ArrayList;

@Slf4j
public class CVConfigurationYamlTest extends WingsBaseTest {
  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testSetEnabled24x7() {
    CVConfigurationYaml yaml = new PrometheusCVConfigurationYaml(new ArrayList<>());
    yaml.setEnabled24x7(true);
    assertThat(yaml.isEnabled24x7()).isTrue();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testSetEnabled24x7WithInteger() {
    CVConfigurationYaml yaml = new PrometheusCVConfigurationYaml(new ArrayList<>());
    assertThatThrownBy(() -> yaml.setEnabled24x7(12)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testSetEnabled24x7WithRandomString() {
    CVConfigurationYaml yaml = new PrometheusCVConfigurationYaml(new ArrayList<>());
    assertThatThrownBy(() -> yaml.setEnabled24x7("Random")).isInstanceOf(IllegalArgumentException.class);
  }
}