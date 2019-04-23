package software.wings.service.intfc.security;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.persistence.UuidAware;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.security.encryption.EncryptionType;
import io.harness.stream.BoundedInputStream;
import software.wings.annotation.EncryptableSetting;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.security.encryption.SecretChangeLog;
import software.wings.security.encryption.SecretUsageLog;
import software.wings.service.impl.security.SecretText;
import software.wings.service.intfc.ownership.OwnedByAccount;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.settings.UsageRestrictions;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Created by rsingh on 10/30/17.
 */
public interface SecretManager extends OwnedByAccount {
  String HARNESS_DEFAULT_SECRET_MANAGER = "Harness Manager";
  String ENCRYPTED_FIELD_MASK = "*******";
  String ACCOUNT_ID_KEY = "accountId";

  List<EncryptionConfig> listEncryptionConfig(String accountId);

  EncryptionType getEncryptionType(String accountId);

  void maskEncryptedFields(EncryptableSetting object);

  void resetUnchangedEncryptedFields(EncryptableSetting sourceObject, EncryptableSetting destinationObject);

  PageResponse<SecretUsageLog> getUsageLogs(PageRequest<SecretUsageLog> pageRequest, String accountId, String entityId,
      SettingVariableTypes variableType) throws IllegalAccessException;

  long getUsageLogsSize(String entityId, SettingVariableTypes variableType) throws IllegalAccessException;

  List<SecretChangeLog> getChangeLogs(String accountId, String entityId, SettingVariableTypes variableType)
      throws IllegalAccessException;

  String encrypt(String accountId, String secret, UsageRestrictions usageRestrictions);

  EncryptedData encrypt(EncryptionType encryptionType, String accountId, SettingVariableTypes settingType,
      char[] secret, String secretPath, EncryptedData encryptedData, String secretName,
      UsageRestrictions usageRestrictions);

  Optional<EncryptedDataDetail> encryptedDataDetails(String accountId, String fieldName, String refId);

  List<EncryptedDataDetail> getEncryptionDetails(EncryptableSetting object, String appId, String workflowExecutionId);

  EncryptionConfig getEncryptionConfig(String accountId, String entityId, EncryptionType encryptionType);

  Collection<UuidAware> listEncryptedValues(String accountId);

  PageResponse<UuidAware> listEncryptedValues(String accountId, PageRequest<EncryptedData> pageRequest);

  String getEncryptedYamlRef(EncryptableSetting object, String... fieldName) throws IllegalAccessException;

  EncryptedData getEncryptedDataFromYamlRef(String encryptedYamlRef, String accountId);

  boolean transitionSecrets(String accountId, EncryptionType fromEncryptionType, String fromSecretId,
      EncryptionType toEncryptionType, String toSecretId);

  void changeSecretManager(String accountId, String entityId, EncryptionType fromEncryptionType, String fromKmsId,
      EncryptionType toEncryptionType, String toKmsId) throws IOException;

  void checkAndAlertForInvalidManagers();

  EncryptedData getSecretByName(String accountId, String name, boolean isMappedToAccount);

  EncryptedData getSecretById(String accountId, String id);

  String saveSecret(String accountId, String name, String value, String path, UsageRestrictions usageRestrictions);

  List<String> importSecrets(String accountId, List<SecretText> secretTexts);

  boolean updateSecret(
      String accountId, String uuId, String name, String value, String path, UsageRestrictions usageRestrictions);

  /**
   *  This method is called when removing application/environment, and all its referring secrets need to clear their
   *  references in usage scope to the application/environment to be deleted.
   */
  boolean updateUsageRestrictionsForSecretOrFile(String accountId, String uuId, UsageRestrictions usageRestrictions);

  boolean deleteSecret(String accountId, String uuId);

  boolean deleteSecretUsingUuid(String uuId);

  String saveFile(String accountId, String name, UsageRestrictions usageRestrictions, BoundedInputStream inputStream);

  File getFile(String accountId, String uuId, File readInto);

  byte[] getFileContents(String accountId, String uuId);

  boolean updateFile(
      String accountId, String name, String uuid, UsageRestrictions usageRestrictions, BoundedInputStream inputStream);

  boolean deleteFile(String accountId, String uuId);

  PageResponse<EncryptedData> listSecrets(String accountId, PageRequest<EncryptedData> pageRequest,
      String appIdFromRequest, String envIdFromRequest, boolean details) throws IllegalAccessException;

  PageResponse<EncryptedData> listSecretsMappedToAccount(
      String accountId, PageRequest<EncryptedData> pageRequest, boolean details) throws IllegalAccessException;

  List<UuidAware> getSecretUsage(String accountId, String secretTextId);

  String saveSecretUsingLocalMode(
      String accountId, String name, String value, String path, UsageRestrictions usageRestrictions);
}
