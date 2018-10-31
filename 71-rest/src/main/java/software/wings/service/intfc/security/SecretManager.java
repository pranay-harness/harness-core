package software.wings.service.intfc.security;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.persistence.UuidAware;
import software.wings.annotation.EncryptableSetting;
import software.wings.security.EncryptionType;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.security.encryption.SecretChangeLog;
import software.wings.security.encryption.SecretUsageLog;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.settings.UsageRestrictions;
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
  String HARNESS_DEFAULT_SECRET_MANAGER = "Harness Manager";
  @SuppressFBWarnings("MS_MUTABLE_ARRAY") char[] ENCRYPTED_FIELD_MASK = "*******".toCharArray();

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
      char[] secret, EncryptedData encryptedData, String secretName, UsageRestrictions usageRestrictions);

  Optional<EncryptedDataDetail> encryptedDataDetails(String accountId, String fieldName, String refId);

  List<EncryptedDataDetail> getEncryptionDetails(EncryptableSetting object, String appId, String workflowExecutionId);

  EncryptionConfig getEncryptionConfig(String accountId, String entityId, EncryptionType encryptionType);

  Collection<UuidAware> listEncryptedValues(String accountId);

  PageResponse<UuidAware> listEncryptedValues(String accountId, PageRequest<EncryptedData> pageRequest);

  String getEncryptedYamlRef(EncryptableSetting object, String... fieldName) throws IllegalAccessException;

  EncryptedData getEncryptedDataFromYamlRef(String encryptedYamlRef) throws IllegalAccessException;

  boolean transitionSecrets(String accountId, EncryptionType fromEncryptionType, String fromSecretId,
      EncryptionType toEncryptionType, String toSecretId);

  void changeSecretManager(String accountId, String entityId, EncryptionType fromEncryptionType, String fromKmsId,
      EncryptionType toEncryptionType, String toKmsId) throws IOException;

  void checkAndAlertForInvalidManagers();

  EncryptedData getSecretMappedToAccountByName(String accountId, String name);

  EncryptedData getSecretMappedToAppByName(String accountId, String appId, String envId, String name);

  EncryptedData getSecretById(String accountId, String id);

  String saveSecret(String accountId, String name, String value, UsageRestrictions usageRestrictions);

  boolean updateSecret(String accountId, String uuId, String name, String value, UsageRestrictions usageRestrictions);

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

  String saveSecretUsingLocalMode(String accountId, String name, String value, UsageRestrictions usageRestrictions);
}
