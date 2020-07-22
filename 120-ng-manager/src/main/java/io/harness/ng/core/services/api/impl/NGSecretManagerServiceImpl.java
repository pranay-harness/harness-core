package io.harness.ng.core.services.api.impl;

import static io.harness.secretmanagerclient.utils.SecretManagerClientUtils.getResponse;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ng.core.services.api.NGSecretManagerService;
import io.harness.secretmanagerclient.dto.NGSecretManagerConfigDTO;
import io.harness.secretmanagerclient.dto.NGSecretManagerConfigUpdateDTO;
import io.harness.secretmanagerclient.remote.SecretManagerClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Singleton
@Slf4j
public class NGSecretManagerServiceImpl implements NGSecretManagerService {
  private final SecretManagerClient secretManagerClient;

  @Override
  public String createSecretManager(NGSecretManagerConfigDTO secretManagerConfig) {
    return getResponse(secretManagerClient.createSecretManager(secretManagerConfig));
  }

  @Override
  public String updateSecretManager(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String identifier, NGSecretManagerConfigUpdateDTO secretManagerConfigUpdateDTO) {
    return getResponse(secretManagerClient.updateSecretManager(
        identifier, accountIdentifier, orgIdentifier, projectIdentifier, secretManagerConfigUpdateDTO));
  }

  @Override
  public List<NGSecretManagerConfigDTO> listSecretManagers(
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return getResponse(secretManagerClient.listSecretManagers(accountIdentifier, orgIdentifier, projectIdentifier));
  }

  @Override
  public NGSecretManagerConfigDTO getSecretManager(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    return getResponse(
        secretManagerClient.getSecretManager(identifier, accountIdentifier, orgIdentifier, projectIdentifier));
  }

  @Override
  public boolean deleteSecretManager(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    return getResponse(
        secretManagerClient.deleteSecretManager(identifier, accountIdentifier, orgIdentifier, projectIdentifier));
  }
}
