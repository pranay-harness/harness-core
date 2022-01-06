/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.azure.registry;

import static io.harness.azure.model.AzureConstants.DOCKER_REGISTRY_SERVER_SECRET_PROPERTY_NAME;
import static io.harness.azure.model.AzureConstants.DOCKER_REGISTRY_SERVER_URL_PROPERTY_NAME;
import static io.harness.azure.model.AzureConstants.DOCKER_REGISTRY_SERVER_USERNAME_PROPERTY_NAME;
import static io.harness.azure.model.AzureConstants.DOCKER_REGISTRY_URL_BLANK_VALIDATION_MSG;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.azure.model.AzureAppServiceApplicationSetting;
import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.exception.InvalidArgumentsException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class AzureRegistry {
  public abstract Optional<DecryptableEntity> getAuthCredentialsDTO(ConnectorConfigDTO connectorConfigDTO);

  public abstract Map<String, AzureAppServiceApplicationSetting> getContainerSettings(
      ConnectorConfigDTO connectorConfigDTO);

  protected Map<String, AzureAppServiceApplicationSetting> populateDockerSettingMap(
      String dockerRegistryUrl, String userName, String decryptedSecret) {
    Map<String, AzureAppServiceApplicationSetting> dockerSettings = new HashMap<>();

    dockerSettings.put(DOCKER_REGISTRY_SERVER_URL_PROPERTY_NAME,
        AzureAppServiceApplicationSetting.builder()
            .name(DOCKER_REGISTRY_SERVER_URL_PROPERTY_NAME)
            .sticky(false)
            .value(dockerRegistryUrl)
            .build());

    dockerSettings.put(DOCKER_REGISTRY_SERVER_USERNAME_PROPERTY_NAME,
        AzureAppServiceApplicationSetting.builder()
            .name(DOCKER_REGISTRY_SERVER_USERNAME_PROPERTY_NAME)
            .sticky(false)
            .value(userName)
            .build());

    dockerSettings.put(DOCKER_REGISTRY_SERVER_SECRET_PROPERTY_NAME,
        AzureAppServiceApplicationSetting.builder()
            .name(DOCKER_REGISTRY_SERVER_SECRET_PROPERTY_NAME)
            .sticky(false)
            .value(decryptedSecret)
            .build());
    return dockerSettings;
  }

  protected Map<String, AzureAppServiceApplicationSetting> populateDockerSettingMap(String dockerRegistryUrl) {
    Map<String, AzureAppServiceApplicationSetting> dockerSettings = new HashMap<>();

    dockerSettings.put(DOCKER_REGISTRY_SERVER_URL_PROPERTY_NAME,
        AzureAppServiceApplicationSetting.builder()
            .name(DOCKER_REGISTRY_SERVER_URL_PROPERTY_NAME)
            .sticky(false)
            .value(dockerRegistryUrl)
            .build());
    return dockerSettings;
  }

  protected void validatePrivateRegistrySettings(String dockerRegistryUrl, String username, String decryptedSecret) {
    validatePublicRegistrySettings(dockerRegistryUrl);

    if (isBlank(username) || isBlank(decryptedSecret)) {
      throw new InvalidArgumentsException(
          "Docker username and password references cannot be empty or null for registries: ACR, Docker Private Hub,"
          + "Private registry");
    }
  }

  protected void validatePublicRegistrySettings(String dockerRegistryUrl) {
    if (isBlank(dockerRegistryUrl)) {
      throw new InvalidArgumentsException(DOCKER_REGISTRY_URL_BLANK_VALIDATION_MSG);
    }
  }
}
