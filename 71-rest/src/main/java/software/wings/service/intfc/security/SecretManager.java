package software.wings.service.intfc.security;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.persistence.UuidAware;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionType;
import io.harness.stream.BoundedInputStream;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.SecretManagerConfig;
import software.wings.beans.SettingAttribute;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.SecretChangeLog;
import software.wings.security.encryption.SecretUsageLog;
import software.wings.service.impl.security.SecretText;
import software.wings.service.intfc.ownership.OwnedByAccount;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.settings.UsageRestrictions;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Created by rsingh on 10/30/17.
 */
public interface SecretManager extends OwnedByAccount {
  String HARNESS_DEFAULT_SECRET_MANAGER = "Harness Manager";
  String ENCRYPTED_FIELD_MASK = "*******";
  String ACCOUNT_ID_KEY = "accountId";
  String SECRET_NAME_KEY = "name";
  String ID_KEY = "_id";
  String IS_DEFAULT_KEY = "isDefault";
  String CREATED_AT_KEY = "createdAt";

  List<SecretManagerConfig> listSecretManagers(String accountId);

  EncryptionType getEncryptionType(String accountId);

  void maskEncryptedFields(EncryptableSetting object);

  void resetUnchangedEncryptedFields(EncryptableSetting sourceObject, EncryptableSetting destinationObject);

  PageResponse<SecretUsageLog> getUsageLogs(PageRequest<SecretUsageLog> pageRequest, String accountId, String entityId,
      SettingVariableTypes variableType) throws IllegalAccessException;

  List<SecretChangeLog> getChangeLogs(String accountId, String entityId, SettingVariableTypes variableType)
      throws IllegalAccessException;

  String encrypt(String accountId, String secret, UsageRestrictions usageRestrictions);

  EncryptedData encrypt(String accountId, SettingVariableTypes settingType, char[] secret, String secretPath,
      EncryptedData encryptedData, String secretName, UsageRestrictions usageRestrictions);

  Optional<EncryptedDataDetail> encryptedDataDetails(String accountId, String fieldName, String refId);

  List<EncryptedDataDetail> getEncryptionDetails(EncryptableSetting object);
  List<EncryptedDataDetail> getEncryptionDetails(EncryptableSetting object, String appId, String workflowExecutionId);

  SecretManagerConfig getSecretManager(String accountId, String entityId, EncryptionType encryptionType);

  Collection<SettingAttribute> listEncryptedSettingAttributes(String accountId);

  Collection<SettingAttribute> listEncryptedSettingAttributes(String accountId, Set<String> categories);

  String getEncryptedYamlRef(EncryptableSetting object, String... fieldName) throws IllegalAccessException;

  EncryptedData getEncryptedDataFromYamlRef(String encryptedYamlRef, String accountId);

  boolean transitionSecrets(String accountId, EncryptionType fromEncryptionType, String fromSecretId,
      EncryptionType toEncryptionType, String toSecretId);

  void changeSecretManager(String accountId, String entityId, EncryptionType fromEncryptionType, String fromKmsId,
      EncryptionType toEncryptionType, String toKmsId) throws IOException;

  EncryptedData getSecretMappedToAccountByName(String accountId, String name);

  EncryptedData getSecretMappedToAppByName(String accountId, String appId, String envId, String name);

  EncryptedData getSecretById(String accountId, String id);

  EncryptedData getSecretByName(String accountId, String name);

  String saveSecret(String accountId, String name, String value, String path, UsageRestrictions usageRestrictions);

  List<String> importSecrets(String accountId, List<SecretText> secretTexts);

  List<String> importSecretsViaFile(String accountId, InputStream uploadStream);

  boolean updateSecret(
      String accountId, String uuId, String name, String value, String path, UsageRestrictions usageRestrictions);

  /**
   *  This method is called when removing application/environment, and all its referring secrets need to clear their
   *  references in usage scope to the application/environment to be deleted.
   */
  boolean updateUsageRestrictionsForSecretOrFile(String accountId, String uuId, UsageRestrictions usageRestrictions);

  boolean deleteSecret(String accountId, String uuId);

  boolean deleteSecretUsingUuid(String uuId);

  String saveFile(String accountId, String name, long fileSize, UsageRestrictions usageRestrictions,
      BoundedInputStream inputStream);

  File getFile(String accountId, String uuId, File readInto);

  byte[] getFileContents(String accountId, String uuId);

  boolean updateFile(String accountId, String name, String uuid, long fileSize, UsageRestrictions usageRestrictions,
      BoundedInputStream inputStream);

  boolean deleteFile(String accountId, String uuId);

  PageResponse<EncryptedData> listSecrets(String accountId, PageRequest<EncryptedData> pageRequest,
      String appIdFromRequest, String envIdFromRequest, boolean details) throws IllegalAccessException;

  PageResponse<EncryptedData> listSecretsMappedToAccount(
      String accountId, PageRequest<EncryptedData> pageRequest, boolean details) throws IllegalAccessException;

  List<UuidAware> getSecretUsage(String accountId, String secretTextId);

  String saveSecretUsingLocalMode(
      String accountId, String name, String value, String path, UsageRestrictions usageRestrictions);

  boolean transitionAllSecretsToHarnessSecretManager(String accountId);

  void clearDefaultFlagOfSecretManagers(String accountId);

  static EncryptedRecordData buildRecordData(EncryptedData encryptedData) {
    return EncryptedRecordData.builder()
        .uuid(encryptedData.getUuid())
        .name(encryptedData.getName())
        .path(encryptedData.getPath())
        .encryptionKey(encryptedData.getEncryptionKey())
        .encryptedValue(encryptedData.getEncryptedValue())
        .kmsId(encryptedData.getKmsId())
        .encryptionType(encryptedData.getEncryptionType())
        .build();
  }
}
