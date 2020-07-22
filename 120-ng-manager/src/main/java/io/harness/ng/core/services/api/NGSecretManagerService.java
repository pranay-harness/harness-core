package io.harness.ng.core.services.api;

import io.harness.secretmanagerclient.dto.NGSecretManagerConfigDTO;
import io.harness.secretmanagerclient.dto.NGSecretManagerConfigUpdateDTO;

import java.util.List;

public interface NGSecretManagerService {
  String createSecretManager(NGSecretManagerConfigDTO secretManagerConfigDTO);

  String updateSecretManager(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String identifier, NGSecretManagerConfigUpdateDTO secretManagerConfigUpdateDTO);

  boolean deleteSecretManager(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  List<NGSecretManagerConfigDTO> listSecretManagers(
      String accountIdentifier, String orgIdentifier, String projectIdentifier);

  NGSecretManagerConfigDTO getSecretManager(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);
}
