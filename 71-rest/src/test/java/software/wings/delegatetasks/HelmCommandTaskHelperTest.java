package software.wings.delegatetasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.delegatetasks.helm.HarnessHelmDeployConfig;
import software.wings.delegatetasks.helm.HelmCommandHelper;
import software.wings.delegatetasks.helm.HelmDeployChartSpec;

import java.util.Optional;

public class HelmCommandTaskHelperTest extends WingsBaseTest {
  @Inject private HelmCommandHelper helmCommandHelper;

  @Test
  @Category(UnitTests.class)
  public void testGenerateHelmDeployChartSpecFromYaml() throws Exception {
    Optional<HarnessHelmDeployConfig> optional = helmCommandHelper.generateHelmDeployChartSpecFromYaml("harness:\n"
        + "    helm:\n"
        + "      chart:\n"
        + "          url: http://storage.googleapis.com/kubernetes-charts\n"
        + "          name: ABC\n"
        + "          version: 0.1.0");

    assertThat(optional.isPresent()).isTrue();
    HelmDeployChartSpec helmDeployChartSpec = optional.get().getHelmDeployChartSpec();
    assertEquals("http://storage.googleapis.com/kubernetes-charts", helmDeployChartSpec.getUrl());
    assertEquals("ABC", helmDeployChartSpec.getName());
    assertEquals("0.1.0", helmDeployChartSpec.getVersion());
  }

  @Test
  @Category(UnitTests.class)
  public void testGenerateHelmDeployChartSpecFromYamlNull() throws Exception {
    Optional<HarnessHelmDeployConfig> optional = helmCommandHelper.generateHelmDeployChartSpecFromYaml("harness:\n"
        + "    helm:\n"
        + "       chart:\n"
        + "          url: http://storage.googleapis.com/kubernetes-charts\n");
    assertThat(optional.isPresent()).isTrue();
    HelmDeployChartSpec helmDeployChartSpec = optional.get().getHelmDeployChartSpec();
    assertEquals("http://storage.googleapis.com/kubernetes-charts", helmDeployChartSpec.getUrl());
    assertThat(helmDeployChartSpec.getName()).isNull();
    assertThat(helmDeployChartSpec.getVersion()).isNull();
  }

  @Test
  @Category(UnitTests.class)
  public void testGenerateHelmDeployChartSpecFromYamlInvalid() throws Exception {
    try {
      Optional<HarnessHelmDeployConfig> optional = helmCommandHelper.generateHelmDeployChartSpecFromYaml("harness:\n"
          + "    helm:\n"
          + "       chart:\n");
      assertFalse(true);
    } catch (Exception e) {
      assertThat(e instanceof WingsException).isTrue();
      assertEquals("Invalid Yaml, Failed while parsing yamlString", e.getMessage());
      assertThat(true).isTrue();
    }
  }

  @Test
  @Category(UnitTests.class)
  public void testGenerateHelmDeployChartSpecFromYamlMultiple() throws Exception {
    Optional<HarnessHelmDeployConfig> optional = helmCommandHelper.generateHelmDeployChartSpecFromYaml("name: ABC\n"
        + "url: http://url.com\n"
        + "---\n"
        + "harness:\n"
        + "    helm:\n"
        + "      chart:\n"
        + "         url: http://storage.googleapis.com/kubernetes-charts\n"
        + "         name: ABC\n"
        + "         version: 0.1.0");

    assertThat(optional.isPresent()).isTrue();
    HelmDeployChartSpec helmDeployChartSpec = optional.get().getHelmDeployChartSpec();
    assertEquals("http://storage.googleapis.com/kubernetes-charts", helmDeployChartSpec.getUrl());
    assertEquals("ABC", helmDeployChartSpec.getName());
    assertEquals("0.1.0", helmDeployChartSpec.getVersion());
  }
}
