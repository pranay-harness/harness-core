package software.wings.service.intfc.security;

import software.wings.beans.CyberArkConfig;
import software.wings.security.encryption.EncryptedData;

public interface CyberArkService {
  char[] decrypt(EncryptedData data, String accountId, CyberArkConfig cyberArkConfig);

  CyberArkConfig getConfig(String accountId, String configId);

  String saveConfig(String accountId, CyberArkConfig cyberArkConfig);

  boolean deleteConfig(String accountId, String configId);

  void validateConfig(CyberArkConfig secretsManagerConfig);

  void decryptCyberArkConfigSecrets(String accountId, CyberArkConfig cyberArkConfig, boolean maskSecret);
}
