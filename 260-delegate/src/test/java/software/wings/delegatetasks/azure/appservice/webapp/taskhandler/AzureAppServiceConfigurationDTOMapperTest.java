/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.delegatetasks.azure.appservice.webapp.taskhandler;

import static io.harness.rule.OwnerRule.IVAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.azure.model.AzureAppServiceApplicationSetting;
import io.harness.azure.model.AzureAppServiceConnectionString;
import io.harness.azure.model.AzureAppServiceConnectionStringType;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.azure.appservicesettings.AzureAppServiceApplicationSettingDTO;
import io.harness.delegate.beans.azure.appservicesettings.AzureAppServiceConnectionStringDTO;
import io.harness.delegate.beans.azure.mapper.AzureAppServiceConfigurationDTOMapper;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@TargetModule(HarnessModule._930_DELEGATE_TASKS)
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
                    .value("HARNESS_APP_SETTING_VALUE")
                    .build());

            put("AZURE_APP_SETTING_NAME",
                AzureAppServiceApplicationSettingDTO.builder()
                    .sticky(true)
                    .name("AZURE_APP_SETTING_NAME")
                    .value("AZURE_APP_SETTING_VALUE")
                    .build());
          }
        };

    Map<String, AzureAppServiceApplicationSetting> appSettings =
        AzureAppServiceConfigurationDTOMapper.getAzureAppServiceAppSettings(appSettingDTOs);

    assertThat(appSettings).isNotNull();
    assertThat(appSettings.size()).isEqualTo(2);

    AzureAppServiceApplicationSetting harnessAppSetting = appSettings.get("HARNESS_APP_SETTING_NAME");
    assertThat(harnessAppSetting).isNotNull();
    assertThat(harnessAppSetting.getName()).isEqualTo("HARNESS_APP_SETTING_NAME");
    assertThat(harnessAppSetting.getValue()).isEqualTo("HARNESS_APP_SETTING_VALUE");
    assertThat(harnessAppSetting.isSticky()).isTrue();

    AzureAppServiceApplicationSetting azureAppSetting = appSettings.get("AZURE_APP_SETTING_NAME");
    assertThat(azureAppSetting).isNotNull();
    assertThat(azureAppSetting.getName()).isEqualTo("AZURE_APP_SETTING_NAME");
    assertThat(azureAppSetting.getValue()).isEqualTo("AZURE_APP_SETTING_VALUE");
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
                    .value("HARNESS_CONN_SETTING_VALUE")
                    .type(AzureAppServiceConnectionStringType.POSTGRE_SQL)
                    .build());

            put("AZURE_APP_SETTING_NAME",
                AzureAppServiceConnectionStringDTO.builder()
                    .sticky(true)
                    .name("AZURE_APP_SETTING_NAME")
                    .value("AZURE_APP_SETTING_VALUE")
                    .type(AzureAppServiceConnectionStringType.SQL_AZURE)
                    .build());
          }
        };

    Map<String, AzureAppServiceConnectionString> connStrings =
        AzureAppServiceConfigurationDTOMapper.getAzureAppServiceConnStrings(connSettingDTOs);

    assertThat(connStrings).isNotNull();
    assertThat(connStrings.size()).isEqualTo(2);

    AzureAppServiceConnectionString harnessConnString = connStrings.get("HARNESS_CONN_SETTING_NAME");
    assertThat(harnessConnString).isNotNull();
    assertThat(harnessConnString.getName()).isEqualTo("HARNESS_CONN_SETTING_NAME");
    assertThat(harnessConnString.getValue()).isEqualTo("HARNESS_CONN_SETTING_VALUE");
    assertThat(harnessConnString.isSticky()).isTrue();
    assertThat(harnessConnString.getType().getValue())
        .isEqualTo(AzureAppServiceConnectionStringType.POSTGRE_SQL.getValue());

    AzureAppServiceConnectionString azureConnString = connStrings.get("AZURE_APP_SETTING_NAME");
    assertThat(azureConnString).isNotNull();
    assertThat(azureConnString.getName()).isEqualTo("AZURE_APP_SETTING_NAME");
    assertThat(azureConnString.getValue()).isEqualTo("AZURE_APP_SETTING_VALUE");
    assertThat(azureConnString.isSticky()).isTrue();
    assertThat(azureConnString.getType().getValue())
        .isEqualTo(AzureAppServiceConnectionStringType.SQL_AZURE.getValue());
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
        AzureAppServiceConfigurationDTOMapper.getAzureAppServiceAppSettingDTOs(appSettings);

    assertThat(appSettingDTOs).isNotNull();
    assertThat(appSettingDTOs.size()).isEqualTo(1);

    AzureAppServiceApplicationSettingDTO applicationSettingDTO = appSettingDTOs.get("AZURE_APP_SETTING_NAME");
    assertThat(applicationSettingDTO).isNotNull();
    assertThat(applicationSettingDTO.getName()).isEqualTo("AZURE_APP_SETTING_NAME");
    assertThat(applicationSettingDTO.isSticky()).isTrue();
    assertThat(applicationSettingDTO.getValue()).isEqualTo("AZURE_APP_SETTING_VALUE");
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
        AzureAppServiceConfigurationDTOMapper.getAzureAppServiceConnStringDTOs(connSettings);

    assertThat(connSettingDTOs).isNotNull();
    assertThat(connSettingDTOs.size()).isEqualTo(1);

    AzureAppServiceConnectionStringDTO connectionSettingDTO = connSettingDTOs.get("AZURE_CONN_SETTING_NAME");
    assertThat(connectionSettingDTO).isNotNull();
    assertThat(connectionSettingDTO.getName()).isEqualTo("AZURE_CONN_SETTING_NAME");
    assertThat(connectionSettingDTO.isSticky()).isTrue();
    assertThat(connectionSettingDTO.getType().getValue())
        .isEqualTo(AzureAppServiceConnectionStringType.SQL_AZURE.getValue());
    assertThat(connectionSettingDTO.getValue()).isEqualTo("AZURE_CONN_SETTING_VALUE");
  }
}
