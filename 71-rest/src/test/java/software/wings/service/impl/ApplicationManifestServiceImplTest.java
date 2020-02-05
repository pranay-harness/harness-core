package software.wings.service.impl;

import static io.harness.rule.OwnerRule.ADWAIT;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.appmanifest.AppManifestKind.K8S_MANIFEST;
import static software.wings.beans.appmanifest.StoreType.HelmChartRepo;
import static software.wings.beans.appmanifest.StoreType.HelmSourceRepo;
import static software.wings.beans.appmanifest.StoreType.Local;
import static software.wings.beans.appmanifest.StoreType.Remote;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.beans.GitFileConfig;
import software.wings.beans.HelmChartConfig;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;

public class ApplicationManifestServiceImplTest extends WingsBaseTest {
  @Spy @InjectMocks ApplicationManifestServiceImpl applicationManifestServiceImpl;

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testValidateAppManifestForEnvironment() {
    ApplicationManifest applicationManifest =
        ApplicationManifest.builder().kind(AppManifestKind.HELM_CHART_OVERRIDE).storeType(Local).build();
    verifyExceptionForValidateAppManifestForEnvironment(applicationManifest);

    applicationManifest.setStoreType(Remote);
    verifyExceptionForValidateAppManifestForEnvironment(applicationManifest);

    applicationManifest.setStoreType(HelmChartRepo);
    applicationManifestServiceImpl.validateAppManifestForEnvironment(applicationManifest);

    applicationManifest.setStoreType(HelmSourceRepo);
    applicationManifestServiceImpl.validateAppManifestForEnvironment(applicationManifest);

    applicationManifest.setKind(K8S_MANIFEST);
    applicationManifest.setStoreType(HelmChartRepo);
    verifyExceptionForValidateAppManifestForEnvironment(applicationManifest);

    applicationManifest.setKind(K8S_MANIFEST);
    applicationManifest.setStoreType(HelmSourceRepo);
    verifyExceptionForValidateAppManifestForEnvironment(applicationManifest);

    applicationManifest.setStoreType(Remote);
    applicationManifestServiceImpl.validateAppManifestForEnvironment(applicationManifest);

    applicationManifest.setStoreType(Local);
    applicationManifestServiceImpl.validateAppManifestForEnvironment(applicationManifest);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testValidateApplicationManifest() {
    ApplicationManifest applicationManifest =
        ApplicationManifest.builder()
            .kind(AppManifestKind.HELM_CHART_OVERRIDE)
            .helmChartConfig(HelmChartConfig.builder().chartName("n").connectorId("c1").build())
            .envId("ENVID")
            .storeType(HelmChartRepo)
            .build();

    try {
      applicationManifestServiceImpl.validateApplicationManifest(applicationManifest);
    } catch (Exception e) {
      assertThat(e instanceof InvalidRequestException).isTrue();
    }

    applicationManifest.setServiceId("s1");
    applicationManifestServiceImpl.validateApplicationManifest(applicationManifest);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testValidateHelmChartRepoAppManifest() {
    ApplicationManifest applicationManifest =
        ApplicationManifest.builder().kind(AppManifestKind.HELM_CHART_OVERRIDE).storeType(HelmChartRepo).build();
    applicationManifest.setGitFileConfig(GitFileConfig.builder().build());
    // No GitConfig
    verifyExceptionForValidateHelmChartRepoAppManifest(applicationManifest);

    HelmChartConfig helmChartConfig = HelmChartConfig.builder().build();
    applicationManifest.setGitFileConfig(null);
    applicationManifest.setHelmChartConfig(helmChartConfig);
    // Empty connectorId and chartName
    verifyExceptionForValidateHelmChartRepoAppManifest(applicationManifest);

    // Empty chartName
    helmChartConfig.setConnectorId("1");
    verifyExceptionForValidateHelmChartRepoAppManifest(applicationManifest);

    // Empty connectorId
    helmChartConfig.setConnectorId(null);
    helmChartConfig.setChartName("Name");
    verifyExceptionForValidateHelmChartRepoAppManifest(applicationManifest);

    // non-empty url (this is invalid, url needs to be empty)
    helmChartConfig.setConnectorId("con1");
    helmChartConfig.setChartName("Name");
    helmChartConfig.setChartUrl("url");
    verifyExceptionForValidateHelmChartRepoAppManifest(applicationManifest);

    helmChartConfig.setChartUrl(null);
    applicationManifestServiceImpl.validateHelmChartRepoAppManifest(applicationManifest);
  }

  private void verifyExceptionForValidateHelmChartRepoAppManifest(ApplicationManifest applicationManifest) {
    try {
      applicationManifestServiceImpl.validateHelmChartRepoAppManifest(applicationManifest);
    } catch (Exception e) {
      assertThat(e instanceof InvalidRequestException).isTrue();
    }
  }

  private void verifyExceptionForValidateAppManifestForEnvironment(ApplicationManifest applicationManifest) {
    try {
      applicationManifestServiceImpl.validateAppManifestForEnvironment(applicationManifest);
    } catch (Exception e) {
      assertThat(e instanceof InvalidRequestException).isTrue();
    }
  }
}
