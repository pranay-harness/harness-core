package software.wings.delegatetasks.helm;

import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.helpers.ext.helm.request.HelmInstallCommandRequest;
import software.wings.helpers.ext.helm.request.HelmReleaseHistoryCommandRequest;
import software.wings.helpers.ext.helm.request.HelmRollbackCommandRequest;

import java.util.Optional;

public class HelmCommandHelperTest extends WingsBaseTest {
  @Inject private HelmCommandHelper helmCommandHelper;

  @Test
  @Owner(developers = ADWAIT)
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
    assertThat(helmDeployChartSpec.getUrl()).isEqualTo("http://storage.googleapis.com/kubernetes-charts");
    assertThat(helmDeployChartSpec.getName()).isEqualTo("ABC");
    assertThat(helmDeployChartSpec.getVersion()).isEqualTo("0.1.0");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGenerateHelmDeployChartSpecFromYamlNull() throws Exception {
    Optional<HarnessHelmDeployConfig> optional = helmCommandHelper.generateHelmDeployChartSpecFromYaml("harness:\n"
        + "    helm:\n"
        + "       chart:\n"
        + "          url: http://storage.googleapis.com/kubernetes-charts\n");
    assertThat(optional.isPresent()).isTrue();
    HelmDeployChartSpec helmDeployChartSpec = optional.get().getHelmDeployChartSpec();
    assertThat(helmDeployChartSpec.getUrl()).isEqualTo("http://storage.googleapis.com/kubernetes-charts");
    assertThat(helmDeployChartSpec.getName()).isNull();
    assertThat(helmDeployChartSpec.getVersion()).isNull();
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGenerateHelmDeployChartSpecFromYamlInvalid() throws Exception {
    try {
      Optional<HarnessHelmDeployConfig> optional = helmCommandHelper.generateHelmDeployChartSpecFromYaml("harness:\n"
          + "    helm:\n"
          + "       chart:\n");
      assertThat(true).isFalse();
    } catch (Exception e) {
      assertThat(e instanceof WingsException).isTrue();
      assertThat(e.getMessage()).isEqualTo("Invalid Yaml, Failed while parsing yamlString");
      assertThat(true).isTrue();
    }
  }

  @Test
  @Owner(developers = ADWAIT)
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
    assertThat(helmDeployChartSpec.getUrl()).isEqualTo("http://storage.googleapis.com/kubernetes-charts");
    assertThat(helmDeployChartSpec.getName()).isEqualTo("ABC");
    assertThat(helmDeployChartSpec.getVersion()).isEqualTo("0.1.0");
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testIsValidChartSpecification() {
    assertThat(helmCommandHelper.isValidChartSpecification(null)).isFalse();
    assertThat(helmCommandHelper.isValidChartSpecification(HelmChartSpecification.builder().build())).isFalse();
    String DUMMY = "DUMMY";
    assertThat(helmCommandHelper.isValidChartSpecification(HelmChartSpecification.builder().chartName(DUMMY).build()))
        .isTrue();
    assertThat(helmCommandHelper.isValidChartSpecification(HelmChartSpecification.builder().chartUrl(DUMMY).build()))
        .isTrue();
    assertThat(helmCommandHelper.isValidChartSpecification(
                   HelmChartSpecification.builder().chartUrl(DUMMY).chartName(DUMMY).build()))
        .isTrue();
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testGetDeploymentMessage() {
    HelmInstallCommandRequest helmInstallCommandRequest = HelmInstallCommandRequest.builder().build();
    assertThat(helmCommandHelper.getDeploymentMessage(helmInstallCommandRequest)).isEqualTo("Installing");

    HelmRollbackCommandRequest helmRollbackCommandRequest = HelmRollbackCommandRequest.builder().build();
    assertThat(helmCommandHelper.getDeploymentMessage(helmRollbackCommandRequest)).isEqualTo("Rolling back");

    HelmReleaseHistoryCommandRequest helmReleaseHistoryCommandRequest =
        HelmReleaseHistoryCommandRequest.builder().build();
    assertThat(helmCommandHelper.getDeploymentMessage(helmReleaseHistoryCommandRequest))
        .isEqualTo("Getting release history");
  }
}
