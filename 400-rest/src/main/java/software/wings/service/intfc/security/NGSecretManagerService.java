package software.wings.service.intfc.security;

import io.harness.beans.SecretManagerConfig;
import io.harness.secretmanagerclient.dto.SecretManagerMetadataDTO;
import io.harness.secretmanagerclient.dto.SecretManagerMetadataRequestDTO;

import java.util.List;
import java.util.Optional;

public interface NGSecretManagerService {
  SecretManagerConfig createSecretManager(SecretManagerConfig secretManagerConfig);

  boolean validate(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  List<SecretManagerConfig> listSecretManagers(
      String accountIdentifier, String orgIdentifier, String projectIdentifier);

  Optional<SecretManagerConfig> getSecretManager(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  SecretManagerConfig updateSecretManager(SecretManagerConfig secretManagerConfig);

  SecretManagerConfig getGlobalSecretManager(String accountIdentifier);

  boolean deleteSecretManager(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  SecretManagerMetadataDTO getMetadata(String accountIdentifier, SecretManagerMetadataRequestDTO requestDTO);
}
