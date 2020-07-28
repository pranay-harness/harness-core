package software.wings.service.intfc.security;

import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.SecretManagerConfig;

import java.util.List;
import java.util.Optional;

public interface NGSecretManagerService {
  SecretManagerConfig createSecretManager(SecretManagerConfig secretManagerConfig);

  List<SecretManagerConfig> listSecretManagers(
      String accountIdentifier, String orgIdentifier, String projectIdentifier);

  Optional<SecretManagerConfig> getSecretManager(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  SecretManagerConfig updateSecretManager(SecretManagerConfig secretManagerConfig);

  List<EncryptedDataDetail> getEncryptionDetails(EncryptableSetting object);

  boolean deleteSecretManager(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);
}
