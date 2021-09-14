/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.ng.core.api.impl;

import static io.harness.NGConstants.HARNESS_SECRET_MANAGER_IDENTIFIER;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.helpers.GlobalSecretManagerUtils.GLOBAL_ACCOUNT_ID;
import static io.harness.remote.client.RestClientUtils.getResponse;
import static io.harness.security.encryption.EncryptionType.AWS_SECRETS_MANAGER;
import static io.harness.security.encryption.EncryptionType.AZURE_VAULT;
import static io.harness.security.encryption.EncryptionType.VAULT;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorValidationResult;
import io.harness.connector.services.NGConnectorSecretManagerService;
import io.harness.connector.services.NGVaultService;
import io.harness.encryptors.KmsEncryptor;
import io.harness.encryptors.KmsEncryptorsRegistry;
import io.harness.encryptors.VaultEncryptorsRegistry;
import io.harness.mappers.SecretManagerConfigMapper;
import io.harness.ng.core.api.NGSecretManagerService;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.secretmanagerclient.dto.SecretManagerConfigUpdateDTO;
import io.harness.secretmanagerclient.dto.SecretManagerMetadataDTO;
import io.harness.secretmanagerclient.dto.SecretManagerMetadataRequestDTO;
import io.harness.secretmanagerclient.remote.SecretManagerClient;
import io.harness.security.encryption.EncryptionConfig;

import software.wings.beans.VaultConfig;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Singleton
@Slf4j
public class NGSecretManagerServiceImpl implements NGSecretManagerService {
  private final SecretManagerClient secretManagerClient;
  private final NGConnectorSecretManagerService ngConnectorSecretManagerService;
  private final KmsEncryptorsRegistry kmsEncryptorsRegistry;
  private final VaultEncryptorsRegistry vaultEncryptorsRegistry;
  private final NGVaultService ngVaultService;

  @Override
  public SecretManagerConfigDTO createSecretManager(@NotNull SecretManagerConfigDTO secretManagerConfig) {
    return getResponse(secretManagerClient.createSecretManager(secretManagerConfig));
  }

  @Override
  public SecretManagerConfigDTO updateSecretManager(@NotNull String accountIdentifier, String orgIdentifier,
      String projectIdentifier, @NotNull String identifier,
      @NotNull SecretManagerConfigUpdateDTO secretManagerConfigUpdateDTO) {
    return getResponse(secretManagerClient.updateSecretManager(
        identifier, accountIdentifier, orgIdentifier, projectIdentifier, secretManagerConfigUpdateDTO));
  }

  @Override
  public SecretManagerConfigDTO getSecretManager(@NotNull String accountIdentifier, String orgIdentifier,
      String projectIdentifier, @NotNull String identifier, boolean maskSecrets) {
    return ngConnectorSecretManagerService.getUsingIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, identifier, maskSecrets);
  }

  private boolean validateNGSecretManager(
      @NotNull String accountIdentifier, SecretManagerConfigDTO secretManagerConfigDTO) {
    boolean validationResult = false;
    if (null != secretManagerConfigDTO) {
      EncryptionConfig encryptionConfig = SecretManagerConfigMapper.fromDTO(secretManagerConfigDTO);
      try {
        switch (encryptionConfig.getType()) {
          case VAULT:
            if (AZURE_VAULT == encryptionConfig.getEncryptionType()
                || AWS_SECRETS_MANAGER == encryptionConfig.getEncryptionType()) {
              validationResult = vaultEncryptorsRegistry.getVaultEncryptor(encryptionConfig.getEncryptionType())
                                     .validateSecretManagerConfiguration(accountIdentifier, encryptionConfig);
            } else {
              VaultConfig vaultConfig = (VaultConfig) encryptionConfig;
              if (!vaultConfig.isReadOnly()) {
                validationResult = vaultEncryptorsRegistry.getVaultEncryptor(VAULT).validateSecretManagerConfiguration(
                    accountIdentifier, vaultConfig);
              }
            }
            break;
          case KMS:
            KmsEncryptor kmsEncryptor = kmsEncryptorsRegistry.getKmsEncryptor(encryptionConfig);
            validationResult = kmsEncryptor.validateKmsConfiguration(encryptionConfig.getAccountId(), encryptionConfig);
            break;
          default:
            String errorMessage = " Encryptor for validate reference task for encryption config"
                + encryptionConfig.getName() + " not configured";
            log.error("Validation failed for Secret Manager/KMS: " + encryptionConfig.getName() + errorMessage);
        }
      } catch (Exception exception) {
        log.error("Validation failed for Secret Manager/KMS: " + encryptionConfig.getName(), exception);
        validationResult = false;
      }
    }
    return validationResult;
  }

  @Override
  public SecretManagerConfigDTO getGlobalSecretManager(String accountIdentifier) {
    try {
      return ngConnectorSecretManagerService.getUsingIdentifier(
          GLOBAL_ACCOUNT_ID, null, null, HARNESS_SECRET_MANAGER_IDENTIFIER, true);
    } catch (Exception e) {
      log.error("Global Secret manager Not found in NG. Calling CG.");
    }
    return getResponse(secretManagerClient.getGlobalSecretManager(accountIdentifier));
  }

  @Override
  public SecretManagerMetadataDTO getMetadata(
      @NotNull String accountIdentifier, SecretManagerMetadataRequestDTO requestDTO) {
    return ngVaultService.getListOfEngines(accountIdentifier, requestDTO);
  }

  @Override
  public ConnectorValidationResult validate(
      @NotNull String accountIdentifier, String orgIdentifier, String projectIdentifier, @NotNull String identifier) {
    ConnectivityStatus connectivityStatus = ConnectivityStatus.FAILURE;
    SecretManagerConfigDTO secretManagerConfigDTO = null;
    try {
      secretManagerConfigDTO = ngConnectorSecretManagerService.getUsingIdentifier(
          accountIdentifier, orgIdentifier, projectIdentifier, identifier, false);
      if (validateNGSecretManager(accountIdentifier, secretManagerConfigDTO)) {
        connectivityStatus = ConnectivityStatus.SUCCESS;
      }
    } catch (Exception exception) {
      log.error("Error getting Connector. Validation false.", exception);
    }
    return ConnectorValidationResult.builder().status(connectivityStatus).build();
  }

  @Override
  public boolean deleteSecretManager(
      @NotNull String accountIdentifier, String orgIdentifier, String projectIdentifier, @NotNull String identifier) {
    return getResponse(
        secretManagerClient.deleteSecretManager(identifier, accountIdentifier, orgIdentifier, projectIdentifier));
  }
}
