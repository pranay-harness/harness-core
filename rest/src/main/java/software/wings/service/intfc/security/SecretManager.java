package software.wings.service.intfc.security;

import software.wings.annotation.Encryptable;
import software.wings.beans.UuidAware;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.security.EncryptionType;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.security.encryption.SecretChangeLog;
import software.wings.security.encryption.SecretUsageLog;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.BoundedInputStream;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Created by rsingh on 10/30/17.
 */
public interface SecretManager {
  List<EncryptionConfig> listEncryptionConfig(String accountId);

  EncryptionType getEncryptionType(String accountId);

  void maskEncryptedFields(Encryptable object);

  void resetUnchangedEncryptedFields(Encryptable sourceObject, Encryptable destinationObject);

  PageResponse<SecretUsageLog> getUsageLogs(PageRequest<SecretUsageLog> pageRequest, String accountId, String entityId,
      SettingVariableTypes variableType) throws IllegalAccessException;

  long getUsageLogsSize(String entityId, SettingVariableTypes variableType) throws IllegalAccessException;

  List<SecretChangeLog> getChangeLogs(String accountId, String entityId, SettingVariableTypes variableType)
      throws IllegalAccessException;

  String encrypt(String accountId, String secret);

  EncryptedData encrypt(EncryptionType encryptionType, String accountId, SettingVariableTypes settingType,
      char[] secret, EncryptedData encryptedData, String secretName);

  Optional<EncryptedDataDetail> encryptedDataDetails(String accountId, String fieldName, String refId);

  List<EncryptedDataDetail> getEncryptionDetails(Encryptable object, String appId, String workflowExecutionId);

  EncryptionConfig getEncryptionConfig(String accountId, String entityId, EncryptionType encryptionType);

  Collection<UuidAware> listEncryptedValues(String accountId);

  String getEncryptedYamlRef(Encryptable object, String... fieldName) throws IllegalAccessException;

  EncryptedData getEncryptedDataFromYamlRef(String encryptedYamlRef) throws IllegalAccessException;

  boolean transitionSecrets(String accountId, EncryptionType fromEncryptionType, String fromSecretId,
      EncryptionType toEncryptionType, String toSecretId);

  void changeSecretManager(String accountId, String entityId, EncryptionType fromEncryptionType, String fromKmsId,
      EncryptionType toEncryptionType, String toKmsId) throws IOException;

  char[] decryptYamlRef(String encryptedYamlRef) throws IllegalAccessException, IOException;

  void checkAndAlertForInvalidManagers();

  String saveSecret(String accountId, String name, String value);

  boolean updateSecret(String accountId, String uuId, String name, String value);

  boolean deleteSecret(String accountId, String uuId);

  String saveFile(String accountId, String name, BoundedInputStream inputStream);

  File getFile(String accountId, String uuId, File readInto);

  String getFileContents(String accountId, String uuId);

  boolean updateFile(String accountId, String name, String uuid, BoundedInputStream inputStream);

  boolean deleteFile(String accountId, String uuId);

  List<EncryptedData> listSecrets(String accountId, SettingVariableTypes type) throws IllegalAccessException;

  List<UuidAware> getSecretUsage(String accountId, String secretTextId);

  String saveSecretUsingLocalMode(String accountId, String name, String value);
}
