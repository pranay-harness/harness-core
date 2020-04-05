package software.wings.service.impl.security;

import static io.harness.data.encoding.EncodingUtils.encodeBase64ToByteArray;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.security.SimpleEncryption.CHARSET;
import static io.harness.security.encryption.EncryptionType.LOCAL;
import static software.wings.service.intfc.FileService.FileBucket.CONFIGS;

import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.data.structure.UUIDGenerator;
import io.harness.security.EncryptionUtils;
import io.harness.security.SimpleEncryption;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.BaseFile;
import software.wings.beans.LocalEncryptionConfig;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.security.LocalEncryptionService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
 * @author marklu on 2019-05-14
 */
@Singleton
@Slf4j
public class LocalEncryptionServiceImpl implements LocalEncryptionService {
  @Inject private FileService fileService;

  @Override
  public EncryptedData encrypt(char[] value, String accountId, LocalEncryptionConfig localEncryptionConfig) {
    char[] encryptedChars = value == null ? null : new SimpleEncryption(accountId).encryptChars(value);
    return EncryptedData.builder()
        .encryptionKey(accountId)
        .encryptedValue(encryptedChars)
        .encryptionType(LOCAL)
        .accountId(accountId)
        .kmsId(localEncryptionConfig.getUuid())
        .enabled(true)
        .build();
  }

  @Override
  public char[] decrypt(EncryptedData data, String accountId, LocalEncryptionConfig localEncryptionConfig) {
    final SimpleEncryption simpleEncryption = new SimpleEncryption(data.getEncryptionKey());
    return simpleEncryption.decryptChars(data.getEncryptedValue());
  }

  @Override
  public LocalEncryptionConfig getEncryptionConfig(String accountId) {
    LocalEncryptionConfig localEncryptionConfig = LocalEncryptionConfig.builder().uuid(accountId).build();
    localEncryptionConfig.setAccountId(accountId);
    return localEncryptionConfig;
    // As LOCAL secret manager is HIDDEN right now. The following 'numOfEncryptedValues' field is not needed
    // Therefore commenting out the code below.
    //    Query<EncryptedData> encryptedDataQuery = wingsPersistence.createQuery(EncryptedData.class)
    //                                                  .filter(EncryptedDataKeys.accountId, accountId)
    //                                                  .filter(EncryptedDataKeys.kmsId, accountId)
    //                                                  .filter(EncryptedDataKeys.encryptionType, LOCAL);
    //    encryptionConfig.setNumOfEncryptedValue(encryptedDataQuery.asKeyList().size());
  }

  @Override
  public EncryptedData encryptFile(
      String accountId, LocalEncryptionConfig localEncryptionConfig, String name, byte[] inputBytes) {
    Preconditions.checkNotNull(localEncryptionConfig);
    byte[] bytes = encodeBase64ToByteArray(inputBytes);
    EncryptedData fileData = encrypt(CHARSET.decode(ByteBuffer.wrap(bytes)).array(), accountId, localEncryptionConfig);
    fileData.setName(name);
    fileData.setAccountId(accountId);
    fileData.setType(SettingVariableTypes.CONFIG_FILE);
    fileData.setBase64Encoded(true);
    fileData.setKmsId(localEncryptionConfig.getUuid());
    char[] encryptedValue = fileData.getEncryptedValue();
    BaseFile baseFile = new BaseFile();
    baseFile.setFileName(name);
    baseFile.setAccountId(accountId);
    baseFile.setFileUuid(UUIDGenerator.generateUuid());
    String fileId = fileService.saveFile(
        baseFile, new ByteArrayInputStream(CHARSET.encode(CharBuffer.wrap(encryptedValue)).array()), CONFIGS);
    fileData.setEncryptedValue(fileId.toCharArray());
    fileData.setFileSize(inputBytes.length);
    return fileData;
  }

  @Override
  public File decryptFile(File file, String accountId, EncryptedData encryptedData) {
    fileService.download(String.valueOf(encryptedData.getEncryptedValue()), file, CONFIGS);
    return EncryptionUtils.decrypt(file, encryptedData.getEncryptionKey(), encryptedData.isBase64Encoded());
  }

  @Override
  public void decryptToStream(String accountId, EncryptedData encryptedData, OutputStream output) {
    File file = new File(Files.createTempDir(), generateUuid());
    logger.info("Temp file path [{}]", file.getAbsolutePath());
    fileService.download(String.valueOf(encryptedData.getEncryptedValue()), file, CONFIGS);
    EncryptionUtils.decryptToStream(file, encryptedData.getEncryptionKey(), output, encryptedData.isBase64Encoded());
  }
}
