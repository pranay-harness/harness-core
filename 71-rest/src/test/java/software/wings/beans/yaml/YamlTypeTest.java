package software.wings.beans.yaml;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.appmanifest.ApplicationManifest;

import java.util.List;
import java.util.regex.Pattern;

public class YamlTypeTest {
  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testGetYamlTypes() {
    List<YamlType> yamlTypes = YamlType.getYamlTypes(ApplicationManifest.class);
    assertThat(yamlTypes).hasSize(10);
    assertThat(yamlTypes).contains(YamlType.APPLICATION_MANIFEST_PCF_ENV_SERVICE_OVERRIDE);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testGetCompiledPatternForYamlTypePathExpression() {
    String expectedPattern =
        Pattern.compile(YamlType.APPLICATION_MANIFEST_OC_PARAMS_ENV_OVERRIDE.getPathExpression()).pattern();

    assertThat(
        YamlType.getCompiledPatternForYamlTypePathExpression(YamlType.APPLICATION_MANIFEST_OC_PARAMS_ENV_OVERRIDE)
            .pattern())
        .isEqualTo(expectedPattern);

    // after caching
    assertThat(
        YamlType.getCompiledPatternForYamlTypePathExpression(YamlType.APPLICATION_MANIFEST_OC_PARAMS_ENV_OVERRIDE)
            .pattern())
        .isEqualTo(expectedPattern);
  }
}