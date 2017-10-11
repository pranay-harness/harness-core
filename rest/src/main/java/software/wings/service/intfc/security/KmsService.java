package software.wings.service.intfc.security;

import software.wings.beans.KmsConfig;
import software.wings.beans.UuidAware;
import software.wings.security.encryption.EncryptedData;

import java.util.Collection;

/**
 * Created by rsingh on 9/29/17.
 */
public interface KmsService {
  EncryptedData encrypt(char[] value, String accountId, KmsConfig kmsConfig);

  char[] decrypt(EncryptedData data, String accountId, KmsConfig kmsConfig);

  KmsConfig getKmsConfig(String accountId);

  boolean saveKmsConfig(String accountId, KmsConfig kmsConfig);

  boolean deleteKmsConfig(String accountId, String kmsConfigId);

  Collection<UuidAware> listEncryptedValues(String accountId);

  boolean transitionKms(String accountId, String fromKmsId, String toKmsId);

  void changeKms(String accountId, String entityId, String fromKmsId, String toKmsId);
}
