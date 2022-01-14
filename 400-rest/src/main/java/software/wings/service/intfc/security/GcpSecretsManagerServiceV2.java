package software.wings.service.intfc.security;

import software.wings.beans.GcpSecretsManagerConfig;

import java.util.List;

public interface GcpSecretsManagerServiceV2 {
  GcpSecretsManagerConfig getGcpSecretsManagerConfig(String accountId, String configId);

  String saveGcpSecretsManagerConfig(
      String accountId, GcpSecretsManagerConfig gcpSecretsManagerConfig, boolean validate);

  String updateGcpSecretsManagerConfig(String accountId, GcpSecretsManagerConfig gcpSecretsManagerConfig);

  boolean deleteGcpSecretsManagerConfig(String accountId, String configId);

  void validateSecretsManagerConfig(String accountId, GcpSecretsManagerConfig GcpSecretsManagerConfig);

  void decryptGcpConfigSecrets(GcpSecretsManagerConfig GcpSecretsManagerConfig, boolean maskSecret);

  List<String> getAllAvailableRegions(String accountId, String configId);
}
