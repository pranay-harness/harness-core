package io.harness.ng.core.api;

import io.harness.secretmanagerclient.dto.NGSecretManagerConfigUpdateDTO;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;

import java.util.List;

public interface NGSecretManagerService {
  SecretManagerConfigDTO createSecretManager(SecretManagerConfigDTO secretManagerConfigDTO);

  SecretManagerConfigDTO updateSecretManager(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String identifier, NGSecretManagerConfigUpdateDTO secretManagerConfigUpdateDTO);

  boolean deleteSecretManager(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  List<SecretManagerConfigDTO> listSecretManagers(
      String accountIdentifier, String orgIdentifier, String projectIdentifier);

  SecretManagerConfigDTO getSecretManager(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);
}
