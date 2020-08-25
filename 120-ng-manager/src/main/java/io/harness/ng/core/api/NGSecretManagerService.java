package io.harness.ng.core.api;

import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.secretmanagerclient.dto.SecretManagerConfigUpdateDTO;

import java.util.List;

public interface NGSecretManagerService {
  SecretManagerConfigDTO createSecretManager(SecretManagerConfigDTO secretManagerConfigDTO);

  SecretManagerConfigDTO updateSecretManager(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String identifier, SecretManagerConfigUpdateDTO secretManagerConfigUpdateDTO);

  boolean deleteSecretManager(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  List<SecretManagerConfigDTO> listSecretManagers(
      String accountIdentifier, String orgIdentifier, String projectIdentifier);

  SecretManagerConfigDTO getSecretManager(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  SecretManagerConfigDTO getGlobalSecretManager(String accountIdentifier);
}
