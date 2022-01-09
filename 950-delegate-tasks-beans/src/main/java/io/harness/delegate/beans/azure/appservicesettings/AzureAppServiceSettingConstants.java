/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.azure.appservicesettings;

public interface AzureAppServiceSettingConstants {
  String AZURE_SETTING_JSON_TYPE = "azureSettingValue";
  String HARNESS_SETTING_SECRET_JSON_TYPE = "harnessSettingSecretValue";
  String HARNESS_SETTING_SECRET_REF_JSON_TYPE = "harnessSettingSecretRef";
  String HARNESS_SETTING_JSON_TYPE = "harnessSettingValue";
  String APPLICATION_SETTING_JSON_TYPE = "applicationSetting";
  String CONNECTION_SETTING_JSON_TYPE = "connectionString";
  String DOCKER_SETTING_JSON_TYPE = "dockerSetting";
}
