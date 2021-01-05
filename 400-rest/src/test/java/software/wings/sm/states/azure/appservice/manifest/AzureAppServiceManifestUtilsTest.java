package software.wings.sm.states.azure.appservice.manifest;

import static io.harness.rule.OwnerRule.IVAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.harness.azure.model.AzureAppServiceApplicationSetting;
import io.harness.azure.model.AzureAppServiceConfiguration;
import io.harness.azure.model.AzureAppServiceConnectionString;
import io.harness.azure.model.AzureAppServiceConnectionStringType;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.Service;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.appmanifest.StoreType;
import software.wings.helpers.ext.k8s.request.K8sValuesLocation;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.states.azure.appservices.manifest.AzureAppServiceManifestUtils;
import software.wings.utils.ApplicationManifestUtils;

import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class AzureAppServiceManifestUtilsTest extends WingsBaseTest {
  @Mock private ApplicationManifestUtils applicationManifestUtils;
  @Mock private ApplicationManifestService applicationManifestService;

  @InjectMocks private AzureAppServiceManifestUtils azureAppServiceManifestUtils;

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testGetAzureAppServiceConfiguration() {
    ExecutionContext mockExecutionContext = mock(ExecutionContext.class);
    getServiceApplicationManifest(mockExecutionContext);

    // appsettings env service override
    Map<K8sValuesLocation, ApplicationManifest> appSettingsEnvServiceOverride = getAppSettingsEnvServiceOverride();
    doReturn(appSettingsEnvServiceOverride)
        .when(applicationManifestUtils)
        .getApplicationManifests(mockExecutionContext, AppManifestKind.AZURE_APP_SETTINGS_OVERRIDE);

    // connstrings env service override
    Map<K8sValuesLocation, ApplicationManifest> connStringsEnvServiceOverride = getConnStringsEnvServiceOverride();
    doReturn(connStringsEnvServiceOverride)
        .when(applicationManifestUtils)
        .getApplicationManifests(mockExecutionContext, AppManifestKind.AZURE_CONN_STRINGS_OVERRIDE);

    AzureAppServiceConfiguration azureAppServiceConfiguration =
        azureAppServiceManifestUtils.getAzureAppServiceConfiguration(mockExecutionContext);

    assertThat(azureAppServiceConfiguration).isNotNull();
    assertThat(azureAppServiceConfiguration.getAppSettingsJSON()).isEqualTo("appsettings-env-service-override");
    assertThat(azureAppServiceConfiguration.getConnStringsJSON()).isEqualTo("connstrings-env-service-override");
  }

  @NotNull
  private ApplicationManifest getServiceApplicationManifest(ExecutionContext mockExecutionContext) {
    Service mockService = mock(Service.class);
    doReturn(mockService).when(applicationManifestUtils).fetchServiceFromContext(mockExecutionContext);

    ApplicationManifest serviceAppManifest = ApplicationManifest.builder().storeType(StoreType.Local).build();
    serviceAppManifest.setAppId("APP_ID");
    serviceAppManifest.setUuid("SERVICE_UUID");
    doReturn(serviceAppManifest).when(applicationManifestService).getByServiceId(anyString(), anyString(), any());

    doReturn(
        ImmutableList.of(
            ManifestFile.builder().fileName("appsettings").fileContent("appsettings-service-app-manifest").build(),
            ManifestFile.builder().fileName("connstrings").fileContent("connstrings-service-app-manifest").build()))
        .when(applicationManifestService)
        .getManifestFilesByAppManifestId(eq("APP_ID"), eq("SERVICE_UUID"));

    return serviceAppManifest;
  }

  private Map<K8sValuesLocation, ApplicationManifest> getAppSettingsEnvServiceOverride() {
    Map<K8sValuesLocation, ApplicationManifest> appSettingsEnvServiceOverride = new HashMap<>();
    ApplicationManifest appSettingsServiceOverride = ApplicationManifest.builder().storeType(StoreType.Local).build();
    appSettingsServiceOverride.setAppId("APP_ID");
    appSettingsServiceOverride.setUuid("APP_SETTINGS_ENV_SERVICE_OVERRIDE_UUID");
    appSettingsEnvServiceOverride.put(K8sValuesLocation.Environment, appSettingsServiceOverride);
    doReturn(
        ImmutableList.of(
            ManifestFile.builder().fileName("appsettings").fileContent("appsettings-env-service-override").build()))
        .when(applicationManifestService)
        .getManifestFilesByAppManifestId(eq("APP_ID"), eq("APP_SETTINGS_ENV_SERVICE_OVERRIDE_UUID"));
    return appSettingsEnvServiceOverride;
  }

  @NotNull
  private Map<K8sValuesLocation, ApplicationManifest> getConnStringsEnvServiceOverride() {
    Map<K8sValuesLocation, ApplicationManifest> connStringsEnvServiceOverride = new HashMap<>();
    ApplicationManifest connStringsServiceOverride = ApplicationManifest.builder().storeType(StoreType.Local).build();
    connStringsServiceOverride.setAppId("APP_ID");
    connStringsServiceOverride.setUuid("CONN_STRINGS_ENV_SERVICE_OVERRIDE_UUID");
    connStringsEnvServiceOverride.put(K8sValuesLocation.Environment, connStringsServiceOverride);
    doReturn(
        ImmutableList.of(
            ManifestFile.builder().fileName("connstrings").fileContent("connstrings-env-service-override").build()))
        .when(applicationManifestService)
        .getManifestFilesByAppManifestId(eq("APP_ID"), eq("CONN_STRINGS_ENV_SERVICE_OVERRIDE_UUID"));
    return connStringsEnvServiceOverride;
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testGetAppSettingSecretsImmutableList() {
    ImmutableList<String> appSettingSecretsImmutableList =
        azureAppServiceManifestUtils.getAppSettingSecretsImmutableList(
            ImmutableList.of(AzureAppServiceApplicationSetting.builder()
                                 .name("SECRET_APP_SETTING")
                                 .value("${secrets.getValue(\"secret_name\")}")
                                 .sticky(true)
                                 .build(),
                AzureAppServiceApplicationSetting.builder()
                    .name("APP_SETTING")
                    .value("APP_SETTING_VALUE")
                    .sticky(true)
                    .build()));

    assertThat(appSettingSecretsImmutableList).isNotEmpty();
    assertThat(appSettingSecretsImmutableList.size()).isEqualTo(1);
    assertThat(appSettingSecretsImmutableList.get(0)).isEqualTo("SECRET_APP_SETTING");
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testGetConnStringSecretsImmutableList() {
    ImmutableList<String> connStringSecretsImmutableList =
        azureAppServiceManifestUtils.getConnStringSecretsImmutableList(
            ImmutableList.of(AzureAppServiceConnectionString.builder()
                                 .name("SECRET_CONN_STRINGS")
                                 .value("${secrets.getValue(\"secret_name\")}")
                                 .type(AzureAppServiceConnectionStringType.CUSTOM)
                                 .sticky(true)
                                 .build(),
                AzureAppServiceConnectionString.builder()
                    .name("CONN_STRINGS")
                    .value("CONN_STRINGS_VALUE")
                    .type(AzureAppServiceConnectionStringType.MYSQL)
                    .sticky(true)
                    .build()));

    assertThat(connStringSecretsImmutableList).isNotEmpty();
    assertThat(connStringSecretsImmutableList.size()).isEqualTo(1);
    assertThat(connStringSecretsImmutableList.get(0)).isEqualTo("SECRET_CONN_STRINGS");
  }
}
