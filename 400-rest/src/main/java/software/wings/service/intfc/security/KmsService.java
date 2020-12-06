package software.wings.service.intfc.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.KmsConfig;

@OwnedBy(PL)
public interface KmsService {
  KmsConfig getKmsConfig(String accountId, String entityId);

  String saveGlobalKmsConfig(String accountId, KmsConfig kmsConfig);

  KmsConfig getGlobalKmsConfig();

  String saveKmsConfig(String accountId, KmsConfig kmsConfig);

  boolean deleteKmsConfig(String accountId, String kmsConfigId);

  void decryptKmsConfigSecrets(String accountId, KmsConfig kmsConfig, boolean maskSecret);
}
