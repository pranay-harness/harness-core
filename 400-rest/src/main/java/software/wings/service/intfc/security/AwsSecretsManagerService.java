package software.wings.service.intfc.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.AwsSecretsManagerConfig;

@OwnedBy(PL)
public interface AwsSecretsManagerService {
  AwsSecretsManagerConfig getAwsSecretsManagerConfig(String accountId, String configId);

  String saveAwsSecretsManagerConfig(String accountId, AwsSecretsManagerConfig secretsManagerConfig);

  boolean deleteAwsSecretsManagerConfig(String accountId, String configId);

  void validateSecretsManagerConfig(AwsSecretsManagerConfig secretsManagerConfig);

  void decryptAsmConfigSecrets(String accountId, AwsSecretsManagerConfig secretsManagerConfig, boolean maskSecret);
}
