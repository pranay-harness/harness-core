package software.wings.service.intfc.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import software.wings.beans.GcpKmsConfig;

@OwnedBy(PL)
public interface GcpSecretsManagerService {
  GcpKmsConfig getGcpKmsConfig(String accountId, String configId);

  String saveGcpKmsConfig(String accountId, GcpKmsConfig gcpKmsConfig, boolean validate);

  String updateGcpKmsConfig(String accountId, GcpKmsConfig gcpKmsConfig, boolean validate);

  String updateGcpKmsConfig(String accountId, GcpKmsConfig gcpKmsConfig);

  GcpKmsConfig getGlobalKmsConfig();

  boolean deleteGcpKmsConfig(String accountId, String configId);

  void validateSecretsManagerConfig(String accountId, GcpKmsConfig gcpKmsConfig);

  void decryptGcpConfigSecrets(GcpKmsConfig gcpKmsConfig, boolean maskSecret);
}
