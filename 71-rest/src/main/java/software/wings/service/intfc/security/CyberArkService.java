package software.wings.service.intfc.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import software.wings.beans.CyberArkConfig;
import software.wings.security.encryption.EncryptedData;

@OwnedBy(PL)
public interface CyberArkService {
  char[] decrypt(EncryptedData data, String accountId, CyberArkConfig cyberArkConfig);

  CyberArkConfig getConfig(String accountId, String configId);

  String saveConfig(String accountId, CyberArkConfig cyberArkConfig);

  boolean deleteConfig(String accountId, String configId);

  void validateConfig(CyberArkConfig secretsManagerConfig);

  void decryptCyberArkConfigSecrets(String accountId, CyberArkConfig cyberArkConfig, boolean maskSecret);
}
