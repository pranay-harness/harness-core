package software.wings.service.impl.yaml.handler.service;

import static io.harness.rule.OwnerRule.ADWAIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static software.wings.beans.appmanifest.AppManifestKind.K8S_MANIFEST;
import static software.wings.beans.appmanifest.AppManifestKind.PCF_OVERRIDE;
import static software.wings.beans.appmanifest.AppManifestKind.VALUES;
import static software.wings.beans.appmanifest.ApplicationManifest.AppManifestSource.ENV;
import static software.wings.beans.appmanifest.ApplicationManifest.AppManifestSource.ENV_SERVICE;
import static software.wings.beans.appmanifest.ApplicationManifest.AppManifestSource.SERVICE;
import static software.wings.beans.appmanifest.StoreType.Local;
import static software.wings.beans.yaml.YamlType.MANIFEST_FILE;
import static software.wings.beans.yaml.YamlType.MANIFEST_FILE_PCF_OVERRIDE_ENV_OVERRIDE;
import static software.wings.beans.yaml.YamlType.MANIFEST_FILE_PCF_OVERRIDE_ENV_SERVICE_OVERRIDE;
import static software.wings.beans.yaml.YamlType.MANIFEST_FILE_VALUES_ENV_OVERRIDE;
import static software.wings.beans.yaml.YamlType.MANIFEST_FILE_VALUES_ENV_SERVICE_OVERRIDE;
import static software.wings.beans.yaml.YamlType.MANIFEST_FILE_VALUES_SERVICE_OVERRIDE;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.yaml.YamlType;
import software.wings.service.intfc.ApplicationManifestService;

public class ManifestFileYamlHandlerTest extends WingsBaseTest {
  @Mock private ApplicationManifestService applicationManifestService;
  @Inject @InjectMocks ManifestFileYamlHandler manifestFileYamlHandler;

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGetManifestFileYamlTypeFromAppManifest() {
    ApplicationManifest applicationManifest = ApplicationManifest.builder().storeType(Local).build();

    doReturn(SERVICE)
        .doReturn(SERVICE)
        .doReturn(ENV)
        .doReturn(ENV)
        .doReturn(ENV)
        .doReturn(ENV_SERVICE)
        .doReturn(ENV_SERVICE)
        .doReturn(ENV_SERVICE)
        .when(applicationManifestService)
        .getAppManifestType(applicationManifest);

    // -----------  AppManifestSource = SERVICE
    applicationManifest.setKind(VALUES);
    YamlType yamlType = manifestFileYamlHandler.getManifestFileYamlTypeFromAppManifest(applicationManifest);
    assertThat(yamlType).isEqualTo(MANIFEST_FILE_VALUES_SERVICE_OVERRIDE);

    applicationManifest.setKind(K8S_MANIFEST);
    yamlType = manifestFileYamlHandler.getManifestFileYamlTypeFromAppManifest(applicationManifest);
    assertThat(yamlType).isEqualTo(MANIFEST_FILE);
    // END -----------  AppManifestSource = SERVICE

    // -----------  AppManifestSource = ENV
    applicationManifest.setKind(VALUES);
    yamlType = manifestFileYamlHandler.getManifestFileYamlTypeFromAppManifest(applicationManifest);
    assertThat(yamlType).isEqualTo(MANIFEST_FILE_VALUES_ENV_OVERRIDE);

    applicationManifest.setKind(PCF_OVERRIDE);
    yamlType = manifestFileYamlHandler.getManifestFileYamlTypeFromAppManifest(applicationManifest);
    assertThat(yamlType).isEqualTo(MANIFEST_FILE_PCF_OVERRIDE_ENV_OVERRIDE);

    applicationManifest.setKind(K8S_MANIFEST);
    try {
      manifestFileYamlHandler.getManifestFileYamlTypeFromAppManifest(applicationManifest);
      fail("Exception expected");
    } catch (Exception e) {
      // Do nothing, expected
    }
    // END -----------  AppManifestSource = ENV

    // -----------  AppManifestSource = ENV_SERVICE
    applicationManifest.setKind(VALUES);
    yamlType = manifestFileYamlHandler.getManifestFileYamlTypeFromAppManifest(applicationManifest);
    assertThat(yamlType).isEqualTo(MANIFEST_FILE_VALUES_ENV_SERVICE_OVERRIDE);

    applicationManifest.setKind(PCF_OVERRIDE);
    yamlType = manifestFileYamlHandler.getManifestFileYamlTypeFromAppManifest(applicationManifest);
    assertThat(yamlType).isEqualTo(MANIFEST_FILE_PCF_OVERRIDE_ENV_SERVICE_OVERRIDE);

    applicationManifest.setKind(K8S_MANIFEST);
    try {
      manifestFileYamlHandler.getManifestFileYamlTypeFromAppManifest(applicationManifest);
      fail("Exception expected");
    } catch (Exception e) {
      // Do nothing, expected
    }
    // END -----------  AppManifestSource = ENV_SERVICE
  }
}
