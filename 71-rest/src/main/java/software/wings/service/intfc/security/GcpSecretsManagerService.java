package software.wings.service.intfc.security;

import software.wings.beans.GcpKmsConfig;

public interface GcpSecretsManagerService {
  GcpKmsConfig getGcpKmsConfig(String accountId, String configId);

  String saveGcpKmsConfig(String accountId, GcpKmsConfig gcpKmsConfig);

  String updateGcpKmsConfig(String accountId, GcpKmsConfig gcpKmsConfig);

  GcpKmsConfig getGlobalKmsConfig();

  boolean deleteGcpKmsConfig(String accountId, String configId);

  void validateSecretsManagerConfig(GcpKmsConfig gcpKmsConfig);

  void decryptGcpConfigSecrets(GcpKmsConfig gcpKmsConfig, boolean maskSecret);
}
