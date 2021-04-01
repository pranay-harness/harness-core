package io.harness.ng.core.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorValidationResult;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.secretmanagerclient.dto.SecretManagerConfigUpdateDTO;
import io.harness.secretmanagerclient.dto.SecretManagerMetadataDTO;
import io.harness.secretmanagerclient.dto.SecretManagerMetadataRequestDTO;

import java.util.List;

@OwnedBy(PL)
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

  ConnectorValidationResult validate(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  SecretManagerConfigDTO getGlobalSecretManager(String accountIdentifier);

  SecretManagerMetadataDTO getMetadata(String accountIdentifier, SecretManagerMetadataRequestDTO requestDTO);
}
