package software.wings.service.impl.security;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.security.encryption.DelegateDecryptionService;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptionConfig;
import software.wings.beans.KmsConfig;
import software.wings.beans.VaultConfig;
import software.wings.security.encryption.SimpleEncryption;
import software.wings.service.intfc.security.SecretManagementDelegateService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author marklu on 2019-02-04
 */
@Singleton
public class DelegateDecryptionServiceImpl implements DelegateDecryptionService {
  @Inject private SecretManagementDelegateService secretManagementDelegateService;

  @Override
  public Map<String, char[]> decrypt(Map<EncryptionConfig, List<EncryptedRecord>> encryptedRecordMap) {
    Map<String, char[]> resultMap = new HashMap<>();
    for (Entry<EncryptionConfig, List<EncryptedRecord>> entry : encryptedRecordMap.entrySet()) {
      EncryptionConfig encryptionConfig = entry.getKey();
      if (encryptionConfig instanceof KmsConfig) {
        for (EncryptedRecord encryptedRecord : entry.getValue()) {
          resultMap.put(encryptedRecord.getUuid(),
              secretManagementDelegateService.decrypt(encryptedRecord, (KmsConfig) encryptionConfig));
        }
      } else if (encryptionConfig instanceof VaultConfig) {
        for (EncryptedRecord encryptedRecord : entry.getValue()) {
          resultMap.put(encryptedRecord.getUuid(),
              secretManagementDelegateService.decrypt(encryptedRecord, (VaultConfig) encryptionConfig));
        }
      } else {
        for (EncryptedRecord encryptedRecord : entry.getValue()) {
          SimpleEncryption encryption = new SimpleEncryption(encryptedRecord.getEncryptionKey());
          resultMap.put(encryptedRecord.getUuid(), encryption.decryptChars(encryptedRecord.getEncryptedValue()));
        }
      }
    }
    return resultMap;
  }
}
