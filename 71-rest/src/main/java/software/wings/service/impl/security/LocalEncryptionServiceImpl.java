package software.wings.service.impl.security;

import static io.harness.security.encryption.EncryptionType.LOCAL;

import software.wings.beans.LocalEncryptionConfig;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.SimpleEncryption;
import software.wings.service.intfc.security.LocalEncryptionService;

import java.util.HashSet;

/**
 * @author marklu on 2019-05-14
 */
public class LocalEncryptionServiceImpl implements LocalEncryptionService {
  @Override
  public EncryptedData encrypt(char[] value, String accountId, LocalEncryptionConfig localEncryptionConfig) {
    char[] encryptedChars = value == null ? null : new SimpleEncryption(accountId).encryptChars(value);
    return EncryptedData.builder()
        .encryptionKey(accountId)
        .encryptedValue(encryptedChars)
        .encryptionType(LOCAL)
        .accountId(accountId)
        .enabled(true)
        .parentIds(new HashSet<>())
        .build();
  }

  @Override
  public char[] decrypt(EncryptedData data, String accountId, LocalEncryptionConfig localEncryptionConfig) {
    final SimpleEncryption simpleEncryption = new SimpleEncryption(data.getEncryptionKey());
    return simpleEncryption.decryptChars(data.getEncryptedValue());
  }

  @Override
  public LocalEncryptionConfig getEncryptionConfig(String accountId) {
    return LocalEncryptionConfig.builder().accountId(accountId).uuid(accountId).build();
    // As LOCAL secret manager is HIDDEN right now. The following 'numOfEncryptedValues' field is not needed
    // Therefore commenting out the code below.
    //    Query<EncryptedData> encryptedDataQuery = wingsPersistence.createQuery(EncryptedData.class)
    //                                                  .filter(EncryptedDataKeys.accountId, accountId)
    //                                                  .filter(EncryptedDataKeys.kmsId, accountId)
    //                                                  .filter(EncryptedDataKeys.encryptionType, LOCAL);
    //    encryptionConfig.setNumOfEncryptedValue(encryptedDataQuery.asKeyList().size());
  }
}
