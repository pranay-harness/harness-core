package software.wings.service.intfc.security;

import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.SecretManagerConfig;

import java.util.List;

public interface NGSecretManagerService {
  String createSecretManager(SecretManagerConfig secretManagerConfig);

  List<SecretManagerConfig> listSecretManagers(
      String accountIdentifier, String orgIdentifier, String projectIdentifier);

  SecretManagerConfig getSecretManager(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  String updateSecretManager(SecretManagerConfig secretManagerConfig);

  List<EncryptedDataDetail> getEncryptionDetails(EncryptableSetting object);

  boolean deleteSecretManager(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);
}
