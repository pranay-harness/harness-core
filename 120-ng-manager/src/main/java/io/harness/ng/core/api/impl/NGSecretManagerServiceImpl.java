package io.harness.ng.core.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.remote.client.RestClientUtils.getResponse;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorValidationResult;
import io.harness.ng.core.api.NGSecretManagerService;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.secretmanagerclient.dto.SecretManagerConfigUpdateDTO;
import io.harness.secretmanagerclient.dto.SecretManagerMetadataDTO;
import io.harness.secretmanagerclient.dto.SecretManagerMetadataRequestDTO;
import io.harness.secretmanagerclient.remote.SecretManagerClient;

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
    return getResponse(secretManagerClient.getSecretManager(
        identifier, accountIdentifier, orgIdentifier, projectIdentifier, maskSecrets));
  }

  @Override
  public SecretManagerConfigDTO getGlobalSecretManager(String accountIdentifier) {
    return getResponse(secretManagerClient.getGlobalSecretManager(accountIdentifier));
  }

  @Override
  public SecretManagerMetadataDTO getMetadata(
      @NotNull String accountIdentifier, SecretManagerMetadataRequestDTO requestDTO) {
    return getResponse(secretManagerClient.getSecretManagerMetadata(accountIdentifier, requestDTO));
  }

  @Override
  public ConnectorValidationResult validate(
      @NotNull String accountIdentifier, String orgIdentifier, String projectIdentifier, @NotNull String identifier) {
    return getResponse(
        secretManagerClient.validateSecretManager(identifier, accountIdentifier, orgIdentifier, projectIdentifier));
  }

  @Override
  public boolean deleteSecretManager(
      @NotNull String accountIdentifier, String orgIdentifier, String projectIdentifier, @NotNull String identifier) {
    return getResponse(
        secretManagerClient.deleteSecretManager(identifier, accountIdentifier, orgIdentifier, projectIdentifier));
  }
}
