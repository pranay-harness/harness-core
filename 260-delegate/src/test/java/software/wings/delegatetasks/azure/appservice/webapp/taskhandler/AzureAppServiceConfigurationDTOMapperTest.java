package software.wings.delegatetasks.azure.appservice.webapp.taskhandler;

import static io.harness.rule.OwnerRule.IVAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.azure.model.AzureAppServiceApplicationSetting;
import io.harness.azure.model.AzureAppServiceConnectionString;
import io.harness.azure.model.AzureAppServiceConnectionStringType;
import io.harness.azure.model.AzureAppServiceDockerSetting;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.azure.appservicesettings.AzureAppServiceApplicationSettingDTO;
import io.harness.delegate.beans.azure.appservicesettings.AzureAppServiceConnectionStringDTO;
import io.harness.delegate.beans.azure.appservicesettings.AzureAppServiceDockerSettingDTO;
import io.harness.delegate.beans.azure.appservicesettings.value.AzureAppServiceAzureSettingValue;
import io.harness.delegate.beans.azure.appservicesettings.value.AzureAppServiceHarnessSettingSecretRef;
import io.harness.delegate.beans.azure.appservicesettings.value.AzureAppServiceHarnessSettingSecretValue;
import io.harness.delegate.beans.azure.appservicesettings.value.AzureAppServiceHarnessSettingValue;
import io.harness.delegate.beans.azure.appservicesettings.value.AzureAppServiceSettingValue;
import io.harness.delegate.beans.azure.appservicesettings.value.AzureAppServiceSettingValueType;
import io.harness.delegate.beans.azure.mapper.AzureAppServiceConfigurationDTOMapper;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@TargetModule(Module._930_DELEGATE_TASKS)
public class AzureAppServiceConfigurationDTOMapperTest extends WingsBaseTest {
  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testGetAzureAppServiceAppSettings() {
    Map<String, AzureAppServiceApplicationSettingDTO> appSettingDTOs =
        new HashMap<String, AzureAppServiceApplicationSettingDTO>() {
          {
            put("HARNESS_APP_SETTING_NAME",
                AzureAppServiceApplicationSettingDTO.builder()
                    .sticky(true)
                    .name("HARNESS_APP_SETTING_NAME")
                    .value(AzureAppServiceHarnessSettingValue.builder().value("HARNESS_APP_SETTING_VALUE").build())
                    .build());

            put("HARNESS_SECRET_APP_SETTING_NAME",
                AzureAppServiceApplicationSettingDTO.builder()
                    .sticky(true)
                    .name("HARNESS_SECRET_APP_SETTING_NAME")
                    .value(AzureAppServiceHarnessSettingSecretValue.builder()
                               .settingSecretRef(
                                   AzureAppServiceHarnessSettingSecretRef.builder()
                                       .secretRef(new SecretRefData("HARNESS_SECRET_APP_SETTING_IDENTIFIER",
                                           Scope.ACCOUNT, "HARNESS_SECRET_APP_SETTING_DECRYPTED_VALUE".toCharArray()))
                                       .build())
                               .encryptedDataDetails(Collections.emptyList())
                               .build())
                    .build());

            put("AZURE_APP_SETTING_NAME",
                AzureAppServiceApplicationSettingDTO.builder()
                    .sticky(true)
                    .name("AZURE_APP_SETTING_NAME")
                    .value(AzureAppServiceAzureSettingValue.builder()
                               .accountId("ACCOUNT_ID")
                               .decryptedValue("AZURE_APP_SETTING_DECRYPTED_VALUE")
                               .build())
                    .build());
          }
        };

    Map<String, AzureAppServiceApplicationSetting> appSettings =
        AzureAppServiceConfigurationDTOMapper.getAzureAppServiceAppSettings(appSettingDTOs);

    assertThat(appSettings).isNotNull();
    assertThat(appSettings.size()).isEqualTo(3);

    AzureAppServiceApplicationSetting harnessAppSetting = appSettings.get("HARNESS_APP_SETTING_NAME");
    assertThat(harnessAppSetting).isNotNull();
    assertThat(harnessAppSetting.getName()).isEqualTo("HARNESS_APP_SETTING_NAME");
    assertThat(harnessAppSetting.getValue()).isEqualTo("HARNESS_APP_SETTING_VALUE");
    assertThat(harnessAppSetting.isSticky()).isTrue();

    AzureAppServiceApplicationSetting harnessSecretAppSetting = appSettings.get("HARNESS_SECRET_APP_SETTING_NAME");
    assertThat(harnessSecretAppSetting).isNotNull();
    assertThat(harnessSecretAppSetting.getName()).isEqualTo("HARNESS_SECRET_APP_SETTING_NAME");
    assertThat(harnessSecretAppSetting.getValue()).isEqualTo("HARNESS_SECRET_APP_SETTING_DECRYPTED_VALUE");
    assertThat(harnessSecretAppSetting.isSticky()).isTrue();

    AzureAppServiceApplicationSetting azureAppSetting = appSettings.get("AZURE_APP_SETTING_NAME");
    assertThat(azureAppSetting).isNotNull();
    assertThat(azureAppSetting.getName()).isEqualTo("AZURE_APP_SETTING_NAME");
    assertThat(azureAppSetting.getValue()).isEqualTo("AZURE_APP_SETTING_DECRYPTED_VALUE");
    assertThat(azureAppSetting.isSticky()).isTrue();
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testGetAzureAppServiceConnStrings() {
    Map<String, AzureAppServiceConnectionStringDTO> connSettingDTOs =
        new HashMap<String, AzureAppServiceConnectionStringDTO>() {
          {
            put("HARNESS_CONN_SETTING_NAME",
                AzureAppServiceConnectionStringDTO.builder()
                    .sticky(true)
                    .name("HARNESS_CONN_SETTING_NAME")
                    .value(AzureAppServiceHarnessSettingValue.builder().value("HARNESS_CONN_SETTING_VALUE").build())
                    .type(AzureAppServiceConnectionStringType.POSTGRE_SQL)
                    .build());

            put("HARNESS_SECRET_CONN_SETTING_NAME",
                AzureAppServiceConnectionStringDTO.builder()
                    .sticky(true)
                    .name("HARNESS_SECRET_CONN_SETTING_NAME")
                    .value(AzureAppServiceHarnessSettingSecretValue.builder()
                               .settingSecretRef(
                                   AzureAppServiceHarnessSettingSecretRef.builder()
                                       .secretRef(new SecretRefData("HARNESS_SECRET_CONN_SETTING_IDENTIFIER",
                                           Scope.ACCOUNT, "HARNESS_SECRET_CONN_SETTING_DECRYPTED_VALUE".toCharArray()))
                                       .build())
                               .encryptedDataDetails(Collections.emptyList())
                               .build())
                    .type(AzureAppServiceConnectionStringType.CUSTOM)
                    .build());

            put("AZURE_CONN_SETTING_NAME",
                AzureAppServiceConnectionStringDTO.builder()
                    .sticky(true)
                    .name("AZURE_CONN_SETTING_NAME")
                    .value(AzureAppServiceAzureSettingValue.builder()
                               .accountId("ACCOUNT_ID")
                               .decryptedValue("AZURE_CONN_SETTING_DECRYPTED_VALUE")
                               .build())
                    .type(AzureAppServiceConnectionStringType.MYSQL)
                    .build());
          }
        };

    Map<String, AzureAppServiceConnectionString> connStrings =
        AzureAppServiceConfigurationDTOMapper.getAzureAppServiceConnStrings(connSettingDTOs);

    assertThat(connStrings).isNotNull();
    assertThat(connStrings.size()).isEqualTo(3);

    AzureAppServiceConnectionString harnessConnString = connStrings.get("HARNESS_CONN_SETTING_NAME");
    assertThat(harnessConnString).isNotNull();
    assertThat(harnessConnString.getName()).isEqualTo("HARNESS_CONN_SETTING_NAME");
    assertThat(harnessConnString.getValue()).isEqualTo("HARNESS_CONN_SETTING_VALUE");
    assertThat(harnessConnString.isSticky()).isTrue();
    assertThat(harnessConnString.getType().getValue())
        .isEqualTo(AzureAppServiceConnectionStringType.POSTGRE_SQL.getValue());

    AzureAppServiceConnectionString harnessSecretConnString = connStrings.get("HARNESS_SECRET_CONN_SETTING_NAME");
    assertThat(harnessSecretConnString).isNotNull();
    assertThat(harnessSecretConnString.getName()).isEqualTo("HARNESS_SECRET_CONN_SETTING_NAME");
    assertThat(harnessSecretConnString.getValue()).isEqualTo("HARNESS_SECRET_CONN_SETTING_DECRYPTED_VALUE");
    assertThat(harnessSecretConnString.isSticky()).isTrue();
    assertThat(harnessSecretConnString.getType().getValue())
        .isEqualTo(AzureAppServiceConnectionStringType.CUSTOM.getValue());

    AzureAppServiceConnectionString azureConnString = connStrings.get("AZURE_CONN_SETTING_NAME");
    assertThat(azureConnString).isNotNull();
    assertThat(azureConnString.getName()).isEqualTo("AZURE_CONN_SETTING_NAME");
    assertThat(azureConnString.getValue()).isEqualTo("AZURE_CONN_SETTING_DECRYPTED_VALUE");
    assertThat(azureConnString.isSticky()).isTrue();
    assertThat(azureConnString.getType().getValue()).isEqualTo(AzureAppServiceConnectionStringType.MYSQL.getValue());
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testGetAzureAppServiceDockerSettings() {
    Map<String, AzureAppServiceDockerSettingDTO> dockerSettingDTOs =
        new HashMap<String, AzureAppServiceDockerSettingDTO>() {
          {
            put("HARNESS_DOCKER_SETTING_NAME",
                AzureAppServiceDockerSettingDTO.builder()
                    .sticky(true)
                    .name("HARNESS_DOCKER_SETTING_NAME")
                    .value(AzureAppServiceHarnessSettingValue.builder().value("HARNESS_DOCKER_SETTING_VALUE").build())
                    .build());

            put("HARNESS_SECRET_DOCKER_SETTING_NAME",
                AzureAppServiceDockerSettingDTO.builder()
                    .sticky(true)
                    .name("HARNESS_SECRET_DOCKER_SETTING_NAME")
                    .value(AzureAppServiceHarnessSettingSecretValue.builder()
                               .settingSecretRef(AzureAppServiceHarnessSettingSecretRef.builder()
                                                     .secretRef(new SecretRefData(
                                                         "HARNESS_SECRET_DOCKER_SETTING_IDENTIFIER", Scope.ACCOUNT,
                                                         "HARNESS_SECRET_DOCKER_SETTING_DECRYPTED_VALUE".toCharArray()))
                                                     .build())
                               .encryptedDataDetails(Collections.emptyList())
                               .build())
                    .build());

            put("AZURE_DOCKER_SETTING_NAME",
                AzureAppServiceDockerSettingDTO.builder()
                    .sticky(true)
                    .name("AZURE_DOCKER_SETTING_NAME")
                    .value(AzureAppServiceAzureSettingValue.builder()
                               .accountId("ACCOUNT_ID")
                               .decryptedValue("AZURE_DOCKER_SETTING_DECRYPTED_VALUE")
                               .build())
                    .build());
          }
        };

    Map<String, AzureAppServiceDockerSetting> dockerSettings =
        AzureAppServiceConfigurationDTOMapper.getAzureAppServiceDockerSettings(dockerSettingDTOs);

    assertThat(dockerSettings).isNotNull();
    assertThat(dockerSettings.size()).isEqualTo(3);

    AzureAppServiceDockerSetting harnessDockerSetting = dockerSettings.get("HARNESS_DOCKER_SETTING_NAME");
    assertThat(harnessDockerSetting).isNotNull();
    assertThat(harnessDockerSetting.getName()).isEqualTo("HARNESS_DOCKER_SETTING_NAME");
    assertThat(harnessDockerSetting.getValue()).isEqualTo("HARNESS_DOCKER_SETTING_VALUE");
    assertThat(harnessDockerSetting.isSticky()).isTrue();

    AzureAppServiceDockerSetting harnessSecretDockerSetting = dockerSettings.get("HARNESS_SECRET_DOCKER_SETTING_NAME");
    assertThat(harnessSecretDockerSetting).isNotNull();
    assertThat(harnessSecretDockerSetting.getName()).isEqualTo("HARNESS_SECRET_DOCKER_SETTING_NAME");
    assertThat(harnessSecretDockerSetting.getValue()).isEqualTo("HARNESS_SECRET_DOCKER_SETTING_DECRYPTED_VALUE");
    assertThat(harnessSecretDockerSetting.isSticky()).isTrue();

    AzureAppServiceDockerSetting azureDockerSetting = dockerSettings.get("AZURE_DOCKER_SETTING_NAME");
    assertThat(azureDockerSetting).isNotNull();
    assertThat(azureDockerSetting.getName()).isEqualTo("AZURE_DOCKER_SETTING_NAME");
    assertThat(azureDockerSetting.getValue()).isEqualTo("AZURE_DOCKER_SETTING_DECRYPTED_VALUE");
    assertThat(azureDockerSetting.isSticky()).isTrue();
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testBuildAzureAppServiceSettingValueWithHarnessSetting() {
    AzureAppServiceSettingValue azureAppServiceSettingValue =
        AzureAppServiceConfigurationDTOMapper.buildAzureAppServiceSettingValue(
            "settingValue", AzureAppServiceSettingValueType.HARNESS_SETTING);

    assertThat(azureAppServiceSettingValue).isNotNull();
    assertThat(azureAppServiceSettingValue).isInstanceOf(AzureAppServiceHarnessSettingValue.class);
    assertThat(((AzureAppServiceHarnessSettingValue) azureAppServiceSettingValue).getValue()).isEqualTo("settingValue");
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testBuildAzureAppServiceSettingValueWithHarnessSettingSecret() {
    AzureAppServiceSettingValue azureAppServiceSettingValue =
        AzureAppServiceConfigurationDTOMapper.buildAzureAppServiceSettingValue(
            "settingValue", AzureAppServiceSettingValueType.HARNESS_SETTING_SECRET);

    assertThat(azureAppServiceSettingValue).isNotNull();
    assertThat(azureAppServiceSettingValue).isInstanceOf(AzureAppServiceHarnessSettingSecretValue.class);

    AzureAppServiceHarnessSettingSecretRef settingSecretRef =
        ((AzureAppServiceHarnessSettingSecretValue) azureAppServiceSettingValue).getSettingSecretRef();
    assertThat(settingSecretRef).isNotNull();

    SecretRefData secretRef = settingSecretRef.getSecretRef();
    assertThat(secretRef).isNotNull();
    assertThat(secretRef.getIdentifier()).isEqualTo("settingValue");
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testBuildAzureAppServiceSettingValueWithAzureSetting() {
    AzureAppServiceSettingValue azureAppServiceSettingValue =
        AzureAppServiceConfigurationDTOMapper.buildAzureAppServiceSettingValue(
            "settingValue", AzureAppServiceSettingValueType.AZURE_SETTING);

    assertThat(azureAppServiceSettingValue).isNotNull();
    assertThat(azureAppServiceSettingValue).isInstanceOf(AzureAppServiceAzureSettingValue.class);
    assertThat(((AzureAppServiceAzureSettingValue) azureAppServiceSettingValue).getDecryptedValue())
        .isEqualTo("settingValue");
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testGetAzureAppServiceAppSettingDTOs() {
    Map<String, AzureAppServiceApplicationSetting> appSettings =
        new HashMap<String, AzureAppServiceApplicationSetting>() {
          {
            put("AZURE_APP_SETTING_NAME",
                AzureAppServiceApplicationSetting.builder()
                    .sticky(true)
                    .name("AZURE_APP_SETTING_NAME")
                    .value("AZURE_APP_SETTING_VALUE")
                    .build());
          }
        };

    Map<String, AzureAppServiceApplicationSettingDTO> appSettingDTOs =
        AzureAppServiceConfigurationDTOMapper.getAzureAppServiceAppSettingDTOs(
            appSettings, AzureAppServiceSettingValueType.AZURE_SETTING);

    assertThat(appSettingDTOs).isNotNull();
    assertThat(appSettingDTOs.size()).isEqualTo(1);

    AzureAppServiceApplicationSettingDTO applicationSettingDTO = appSettingDTOs.get("AZURE_APP_SETTING_NAME");
    assertThat(applicationSettingDTO).isNotNull();
    assertThat(applicationSettingDTO.getName()).isEqualTo("AZURE_APP_SETTING_NAME");
    assertThat(applicationSettingDTO.isSticky()).isTrue();
    assertThat(applicationSettingDTO.getValue()).isInstanceOf(AzureAppServiceAzureSettingValue.class);
    assertThat(((AzureAppServiceAzureSettingValue) applicationSettingDTO.getValue()).getDecryptedValue())
        .isEqualTo("AZURE_APP_SETTING_VALUE");
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testGetAzureAppServiceConnStringDTOs() {
    Map<String, AzureAppServiceConnectionString> connSettings = new HashMap<String, AzureAppServiceConnectionString>() {
      {
        put("AZURE_CONN_SETTING_NAME",
            AzureAppServiceConnectionString.builder()
                .sticky(true)
                .name("AZURE_CONN_SETTING_NAME")
                .value("AZURE_CONN_SETTING_VALUE")
                .type(AzureAppServiceConnectionStringType.SQL_AZURE)
                .build());
      }
    };

    Map<String, AzureAppServiceConnectionStringDTO> connSettingDTOs =
        AzureAppServiceConfigurationDTOMapper.getAzureAppServiceConnStringDTOs(
            connSettings, AzureAppServiceSettingValueType.AZURE_SETTING);

    assertThat(connSettingDTOs).isNotNull();
    assertThat(connSettingDTOs.size()).isEqualTo(1);

    AzureAppServiceConnectionStringDTO connectionSettingDTO = connSettingDTOs.get("AZURE_CONN_SETTING_NAME");
    assertThat(connectionSettingDTO).isNotNull();
    assertThat(connectionSettingDTO.getName()).isEqualTo("AZURE_CONN_SETTING_NAME");
    assertThat(connectionSettingDTO.isSticky()).isTrue();
    assertThat(connectionSettingDTO.getType().getValue())
        .isEqualTo(AzureAppServiceConnectionStringType.SQL_AZURE.getValue());
    assertThat(connectionSettingDTO.getValue()).isInstanceOf(AzureAppServiceAzureSettingValue.class);
    assertThat(((AzureAppServiceAzureSettingValue) connectionSettingDTO.getValue()).getDecryptedValue())
        .isEqualTo("AZURE_CONN_SETTING_VALUE");
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testGetAzureAppServiceDockerSettingDTOs() {
    Map<String, AzureAppServiceDockerSetting> connSettings = new HashMap<String, AzureAppServiceDockerSetting>() {
      {
        put("AZURE_DOCKER_SETTING_NAME",
            AzureAppServiceDockerSetting.builder()
                .sticky(true)
                .name("AZURE_DOCKER_SETTING_NAME")
                .value("AZURE_DOCKER_SETTING_VALUE")
                .build());
      }
    };

    Map<String, AzureAppServiceDockerSettingDTO> dockerSettingDTOs =
        AzureAppServiceConfigurationDTOMapper.getAzureAppServiceDockerSettingDTOs(
            connSettings, AzureAppServiceSettingValueType.AZURE_SETTING);

    assertThat(dockerSettingDTOs).isNotNull();
    assertThat(dockerSettingDTOs.size()).isEqualTo(1);

    AzureAppServiceDockerSettingDTO dockerSettingDTO = dockerSettingDTOs.get("AZURE_DOCKER_SETTING_NAME");
    assertThat(dockerSettingDTO).isNotNull();
    assertThat(dockerSettingDTO.getName()).isEqualTo("AZURE_DOCKER_SETTING_NAME");
    assertThat(dockerSettingDTO.isSticky()).isTrue();
    assertThat(dockerSettingDTO.getValue()).isInstanceOf(AzureAppServiceAzureSettingValue.class);
    assertThat(((AzureAppServiceAzureSettingValue) dockerSettingDTO.getValue()).getDecryptedValue())
        .isEqualTo("AZURE_DOCKER_SETTING_VALUE");
  }
}
