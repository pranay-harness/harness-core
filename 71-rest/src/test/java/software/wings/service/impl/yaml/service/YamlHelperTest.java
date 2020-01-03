package software.wings.service.impl.yaml.service;

import static io.harness.rule.OwnerRule.ADWAIT;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.appmanifest.AppManifestKind.PCF_OVERRIDE;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

public class YamlHelperTest extends WingsBaseTest {
  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGetAppManifestKindFromPath() {
    YamlHelper yamlHelper = new YamlHelper();
    assertThat(
        yamlHelper.getAppManifestKindFromPath("Setup/Applications/App1/Environments/env1/PCF Overrides/Index.yaml"))
        .isEqualTo(PCF_OVERRIDE);

    assertThat(
        yamlHelper.getAppManifestKindFromPath("Setup/Applications/App1/Environments/env1/PCF Overrides/Services"))
        .isEqualTo(PCF_OVERRIDE);
  }
}
