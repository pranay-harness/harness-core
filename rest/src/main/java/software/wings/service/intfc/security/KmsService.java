package software.wings.service.intfc.security;

import software.wings.beans.KmsConfig;
import software.wings.beans.UuidAware;
import software.wings.annotation.Encryptable;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.utils.BoundedInputStream;

import java.io.File;
import java.util.Collection;
import java.util.List;

/**
 * Created by rsingh on 9/29/17.
 */
public interface KmsService {
  boolean shouldUseKms(String accountId);

  EncryptedData encrypt(char[] value, String accountId, KmsConfig kmsConfig);

  char[] decrypt(EncryptedData data, String accountId, KmsConfig kmsConfig);

  KmsConfig getKmsConfig(String accountId);

  boolean saveGlobalKmsConfig(String accountId, KmsConfig kmsConfig);

  boolean saveKmsConfig(String accountId, KmsConfig kmsConfig);

  boolean deleteKmsConfig(String accountId, String kmsConfigId);

  Collection<UuidAware> listEncryptedValues(String accountId);

  Collection<KmsConfig> listKmsConfigs(String accountId);

  boolean transitionKms(String accountId, String fromKmsId, String toKmsId);

  void changeKms(String accountId, String entityId, String fromKmsId, String toKmsId);

  EncryptedData encryptFile(BoundedInputStream inputStream, String accountId);

  File decryptFile(File file, String accountId, EncryptedData encryptedData);

  List<EncryptedDataDetail> getEncryptionDetails(Encryptable object, String workFlowId, String appId);

  String getEncryptedYamlRef(Encryptable object, String appId, String fieldName, String entityName)
      throws IllegalAccessException;

  char[] decryptYamlRef(String encryptedYamlRef) throws NoSuchFieldException, IllegalAccessException;
}
