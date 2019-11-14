package software.wings.verification;

import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.reflections.Reflections;
import software.wings.WingsBaseTest;
import software.wings.verification.CVConfiguration.CVConfigurationYaml;

import java.util.Set;

public class CVConfigurationYamlHandlerTest extends WingsBaseTest {
  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testYamlClass() {
    Reflections reflections = new Reflections("software.wings");
    Set<Class<? extends CVConfigurationYamlHandler>> yamlHandlerClasses =
        reflections.getSubTypesOf(CVConfigurationYamlHandler.class);
    assertThat(yamlHandlerClasses.size()).isGreaterThan(0);

    yamlHandlerClasses.forEach(yamlHandlerClass -> {
      try {
        assertThat(CVConfigurationYaml.class).isAssignableFrom(yamlHandlerClass.newInstance().getYamlClass());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }
}
