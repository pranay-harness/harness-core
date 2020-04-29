package software.wings.service.impl.yaml;

import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static software.wings.beans.appmanifest.AppManifestKind.HELM_CHART_OVERRIDE;
import static software.wings.beans.appmanifest.AppManifestKind.K8S_MANIFEST;
import static software.wings.beans.appmanifest.AppManifestKind.OC_PARAMS;
import static software.wings.beans.appmanifest.AppManifestKind.PCF_OVERRIDE;
import static software.wings.beans.appmanifest.AppManifestKind.VALUES;
import static software.wings.beans.appmanifest.ApplicationManifest.AppManifestSource.ENV;
import static software.wings.beans.appmanifest.ApplicationManifest.AppManifestSource.ENV_SERVICE;
import static software.wings.beans.appmanifest.ApplicationManifest.AppManifestSource.SERVICE;
import static software.wings.beans.appmanifest.StoreType.Local;
import static software.wings.beans.yaml.YamlType.APPLICATION_MANIFEST;
import static software.wings.beans.yaml.YamlType.APPLICATION_MANIFEST_HELM_ENV_SERVICE_OVERRIDE;
import static software.wings.beans.yaml.YamlType.APPLICATION_MANIFEST_HELM_OVERRIDES_ALL_SERVICE;
import static software.wings.beans.yaml.YamlType.APPLICATION_MANIFEST_OC_PARAMS_ENV_OVERRIDE;
import static software.wings.beans.yaml.YamlType.APPLICATION_MANIFEST_OC_PARAMS_ENV_SERVICE_OVERRIDE;
import static software.wings.beans.yaml.YamlType.APPLICATION_MANIFEST_PCF_ENV_SERVICE_OVERRIDE;
import static software.wings.beans.yaml.YamlType.APPLICATION_MANIFEST_PCF_OVERRIDES_ALL_SERVICE;
import static software.wings.beans.yaml.YamlType.APPLICATION_MANIFEST_VALUES_ENV_OVERRIDE;
import static software.wings.beans.yaml.YamlType.APPLICATION_MANIFEST_VALUES_ENV_SERVICE_OVERRIDE;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.yaml.YamlType;
import software.wings.service.intfc.ApplicationManifestService;

public class YamlResourceServiceImplTest extends WingsBaseTest {
  @Mock ApplicationManifestService applicationManifestService;
  @InjectMocks @Inject YamlResourceServiceImpl yamlResourceService;

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGetManifestFileYamlTypeFromAppManifest() {
    ApplicationManifest applicationManifest = ApplicationManifest.builder().storeType(Local).build();

    doReturn(SERVICE)
        .doReturn(ENV)
        .doReturn(ENV)
        .doReturn(ENV)
        .doReturn(ENV_SERVICE)
        .doReturn(ENV_SERVICE)
        .doReturn(ENV_SERVICE)
        .when(applicationManifestService)
        .getAppManifestType(applicationManifest);

    // -----------  AppManifestSource = SERVICE
    YamlType yamlType = yamlResourceService.getYamlTypeFromAppManifest(applicationManifest);
    assertThat(yamlType).isEqualTo(APPLICATION_MANIFEST);
    // END -----------  AppManifestSource = SERVICE

    // -----------  AppManifestSource = ENV
    applicationManifest.setKind(VALUES);
    yamlType = yamlResourceService.getYamlTypeFromAppManifest(applicationManifest);
    assertThat(yamlType).isEqualTo(APPLICATION_MANIFEST_VALUES_ENV_OVERRIDE);

    applicationManifest.setKind(PCF_OVERRIDE);
    yamlType = yamlResourceService.getYamlTypeFromAppManifest(applicationManifest);
    assertThat(yamlType).isEqualTo(APPLICATION_MANIFEST_PCF_OVERRIDES_ALL_SERVICE);

    applicationManifest.setKind(K8S_MANIFEST);
    try {
      yamlResourceService.getYamlTypeFromAppManifest(applicationManifest);
      fail("Exception expected");
    } catch (Exception e) {
      // Do nothing, expected
    }
    // END -----------  AppManifestSource = ENV

    // -----------  AppManifestSource = ENV_SERVICE
    applicationManifest.setKind(VALUES);
    yamlType = yamlResourceService.getYamlTypeFromAppManifest(applicationManifest);
    assertThat(yamlType).isEqualTo(APPLICATION_MANIFEST_VALUES_ENV_SERVICE_OVERRIDE);

    applicationManifest.setKind(PCF_OVERRIDE);
    yamlType = yamlResourceService.getYamlTypeFromAppManifest(applicationManifest);
    assertThat(yamlType).isEqualTo(APPLICATION_MANIFEST_PCF_ENV_SERVICE_OVERRIDE);

    applicationManifest.setKind(K8S_MANIFEST);
    try {
      yamlResourceService.getYamlTypeFromAppManifest(applicationManifest);
      fail("Exception expected");
    } catch (Exception e) {
      // Do nothing, expected
    }
    // END -----------  AppManifestSource = ENV_SERVICE
  }

  private void testGetYamlTypeFromAppManifestForValuesKind() {
    ApplicationManifest applicationManifest =
        ApplicationManifest.builder().serviceId(SERVICE_ID).kind(VALUES).storeType(StoreType.Local).build();
    doReturn(SERVICE).when(applicationManifestService).getAppManifestType(applicationManifest);

    YamlType yamlType = yamlResourceService.getYamlTypeFromAppManifest(applicationManifest);

    assertThat(yamlType).isEqualTo(YamlType.APPLICATION_MANIFEST);

    applicationManifest.setEnvId(ENV_ID);
    doReturn(ENV_SERVICE).when(applicationManifestService).getAppManifestType(applicationManifest);
    yamlType = yamlResourceService.getYamlTypeFromAppManifest(applicationManifest);
    assertThat(yamlType).isEqualTo(APPLICATION_MANIFEST_VALUES_ENV_SERVICE_OVERRIDE);

    applicationManifest.setServiceId(null);
    doReturn(ENV).when(applicationManifestService).getAppManifestType(applicationManifest);
    yamlType = yamlResourceService.getYamlTypeFromAppManifest(applicationManifest);
    assertThat(yamlType).isEqualTo(APPLICATION_MANIFEST_VALUES_ENV_OVERRIDE);
  }

  private void testGetYamlTypeFromAppManifestForParamsKind() {
    ApplicationManifest applicationManifest =
        ApplicationManifest.builder().serviceId(SERVICE_ID).kind(OC_PARAMS).storeType(StoreType.Local).build();
    doReturn(SERVICE).when(applicationManifestService).getAppManifestType(applicationManifest);

    YamlType yamlType = yamlResourceService.getYamlTypeFromAppManifest(applicationManifest);

    assertThat(yamlType).isEqualTo(YamlType.APPLICATION_MANIFEST);

    applicationManifest.setEnvId(ENV_ID);
    doReturn(ENV_SERVICE).when(applicationManifestService).getAppManifestType(applicationManifest);
    yamlType = yamlResourceService.getYamlTypeFromAppManifest(applicationManifest);
    assertThat(yamlType).isEqualTo(APPLICATION_MANIFEST_OC_PARAMS_ENV_SERVICE_OVERRIDE);

    applicationManifest.setServiceId(null);
    doReturn(ENV).when(applicationManifestService).getAppManifestType(applicationManifest);
    yamlType = yamlResourceService.getYamlTypeFromAppManifest(applicationManifest);
    assertThat(yamlType).isEqualTo(APPLICATION_MANIFEST_OC_PARAMS_ENV_OVERRIDE);
  }

  private void testGetYamlTypeFromAppManifestForHelmKind() {
    ApplicationManifest applicationManifest = ApplicationManifest.builder()
                                                  .serviceId(SERVICE_ID)
                                                  .kind(HELM_CHART_OVERRIDE)
                                                  .storeType(StoreType.Local)
                                                  .build();
    doReturn(SERVICE).when(applicationManifestService).getAppManifestType(applicationManifest);

    YamlType yamlType = yamlResourceService.getYamlTypeFromAppManifest(applicationManifest);

    assertThat(yamlType).isEqualTo(YamlType.APPLICATION_MANIFEST);

    applicationManifest.setEnvId(ENV_ID);
    doReturn(ENV_SERVICE).when(applicationManifestService).getAppManifestType(applicationManifest);
    yamlType = yamlResourceService.getYamlTypeFromAppManifest(applicationManifest);
    assertThat(yamlType).isEqualTo(APPLICATION_MANIFEST_HELM_ENV_SERVICE_OVERRIDE);

    applicationManifest.setServiceId(null);
    doReturn(ENV).when(applicationManifestService).getAppManifestType(applicationManifest);
    yamlType = yamlResourceService.getYamlTypeFromAppManifest(applicationManifest);
    assertThat(yamlType).isEqualTo(APPLICATION_MANIFEST_HELM_OVERRIDES_ALL_SERVICE);
  }

  private void testGetYamlTypeFromAppManifestForPCFKind() {
    ApplicationManifest applicationManifest =
        ApplicationManifest.builder().serviceId(SERVICE_ID).kind(PCF_OVERRIDE).storeType(StoreType.Local).build();
    doReturn(SERVICE).when(applicationManifestService).getAppManifestType(applicationManifest);

    YamlType yamlType = yamlResourceService.getYamlTypeFromAppManifest(applicationManifest);

    assertThat(yamlType).isEqualTo(YamlType.APPLICATION_MANIFEST);

    applicationManifest.setEnvId(ENV_ID);
    doReturn(ENV_SERVICE).when(applicationManifestService).getAppManifestType(applicationManifest);
    yamlType = yamlResourceService.getYamlTypeFromAppManifest(applicationManifest);
    assertThat(yamlType).isEqualTo(APPLICATION_MANIFEST_PCF_ENV_SERVICE_OVERRIDE);

    applicationManifest.setServiceId(null);
    doReturn(ENV).when(applicationManifestService).getAppManifestType(applicationManifest);
    yamlType = yamlResourceService.getYamlTypeFromAppManifest(applicationManifest);
    assertThat(yamlType).isEqualTo(APPLICATION_MANIFEST_PCF_OVERRIDES_ALL_SERVICE);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testGetYamlTypeFromAppManifest() {
    testGetYamlTypeFromAppManifestForValuesKind();
    testGetYamlTypeFromAppManifestForParamsKind();
    testGetYamlTypeFromAppManifestForHelmKind();
    testGetYamlTypeFromAppManifestForPCFKind();
    shouldThrowExceptionForNullKind();
    shouldThrowExceptionForUnknownKind();
  }

  private void shouldThrowExceptionForUnknownKind() {
    ApplicationManifest applicationManifest =
        ApplicationManifest.builder().envId(ENV_ID).kind(K8S_MANIFEST).storeType(StoreType.Local).build();
    doReturn(ENV).when(applicationManifestService).getAppManifestType(applicationManifest);

    assertThatThrownBy(() -> yamlResourceService.getYamlTypeFromAppManifest(applicationManifest))
        .hasMessageContaining("Invalid ApplicationManifestKind: K8S_MANIFEST");
  }

  private void shouldThrowExceptionForNullKind() {
    ApplicationManifest applicationManifest =
        ApplicationManifest.builder().envId(ENV_ID).storeType(StoreType.Local).build();
    doReturn(ENV).when(applicationManifestService).getAppManifestType(applicationManifest);

    assertThatThrownBy(() -> yamlResourceService.getYamlTypeFromAppManifest(applicationManifest))
        .hasMessageContaining("ApplicationManifest Kind can not be null");
  }
}
