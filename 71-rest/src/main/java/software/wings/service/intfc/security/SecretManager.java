package software.wings.service.intfc.security;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.exception.WingsException.USER;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionType;
import io.harness.stream.BoundedInputStream;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.Base;
import software.wings.beans.SecretManagerConfig;
import software.wings.beans.SecretManagerRuntimeParameters;
import software.wings.beans.SettingAttribute;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.SecretChangeLog;
import software.wings.security.encryption.SecretUsageLog;
import software.wings.security.encryption.setupusage.SecretSetupUsage;
import software.wings.service.impl.security.SecretManagementException;
import software.wings.service.impl.security.SecretText;
import software.wings.service.intfc.ownership.OwnedByAccount;
import software.wings.settings.SettingVariableTypes;
import software.wings.settings.UsageRestrictions;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.validation.constraints.NotNull;

/**
 * Created by rsingh on 10/30/17.
 */
public interface SecretManager extends OwnedByAccount {
  String HARNESS_DEFAULT_SECRET_MANAGER = "Harness Secrets Manager";
  String ENCRYPTED_FIELD_MASK = "*******";
  String ACCOUNT_ID_KEY = "accountId";
  String SECRET_NAME_KEY = "name";
  String ID_KEY = "_id";
  String IS_DEFAULT_KEY = "isDefault";
  String CREATED_AT_KEY = "createdAt";
  String ENCRYPTION_TYPE_KEY = "encryptionType";

  List<SecretManagerConfig> listSecretManagers(String accountId);

  SecretManagerConfig getSecretManager(String accountId, String kmsId);

  EncryptionType getEncryptionType(String accountId);

  EncryptionType getEncryptionBySecretManagerId(String kmsId, String accountId);

  void maskEncryptedFields(EncryptableSetting object);

  void resetUnchangedEncryptedFields(EncryptableSetting sourceObject, EncryptableSetting destinationObject);

  PageResponse<SecretUsageLog> getUsageLogs(PageRequest<SecretUsageLog> pageRequest, String accountId, String entityId,
      SettingVariableTypes variableType) throws IllegalAccessException;

  Set<SecretSetupUsage> getSecretUsage(String accountId, String secretId);

  List<SecretChangeLog> getChangeLogs(String accountId, String entityId, SettingVariableTypes variableType)
      throws IllegalAccessException;

  String encrypt(String accountId, String secret, UsageRestrictions usageRestrictions);

  EncryptedData encrypt(String accountId, SettingVariableTypes settingType, char[] secret, EncryptedData encryptedData,
      SecretText secretText);

  Optional<EncryptedDataDetail> encryptedDataDetails(
      String accountId, String fieldName, String refId, String workflowExecutionId);

  List<EncryptedDataDetail> getEncryptionDetails(EncryptableSetting object);

  List<EncryptedDataDetail> getEncryptionDetails(EncryptableSetting object, String appId, String workflowExecutionId);

  SecretManagerConfig getSecretManager(String accountId, String entityId, EncryptionType encryptionType);

  Collection<SettingAttribute> listEncryptedSettingAttributes(String accountId);

  Collection<SettingAttribute> listEncryptedSettingAttributes(String accountId, Set<String> categories);

  String getEncryptedYamlRef(EncryptableSetting object, String... fieldName) throws IllegalAccessException;

  EncryptedData getEncryptedDataFromYamlRef(String encryptedYamlRef, String accountId);

  boolean transitionSecrets(String accountId, EncryptionType fromEncryptionType, String fromSecretId,
      EncryptionType toEncryptionType, String toSecretId);

  boolean transitionSecrets(String accountId, EncryptionType fromEncryptionType, String fromManagerSecretId,
      EncryptionType toEncryptionType, String toSecretManagerId,
      Map<String, String> runtimeParametersForSourceSecretManager,
      Map<String, String> runtimeParametersForDestinationSecretManager);

  void changeSecretManager(String accountId, String entityId, EncryptionType fromEncryptionType, String fromKmsId,
      EncryptionType toEncryptionType, String toKmsId) throws IOException;

  EncryptedData getSecretMappedToAccountByName(String accountId, String name);

  EncryptedData getSecretMappedToAppByName(String accountId, String appId, String envId, String name);

  EncryptedData getSecretById(String accountId, String id);

  EncryptedData getSecretByName(String accountId, String name);

  String saveSecret(String accountId, SecretText secretText);

  List<String> importSecrets(String accountId, List<SecretText> secretTexts);

  List<String> importSecretsViaFile(String accountId, InputStream uploadStream);

  void validateThatSecretManagerSupportsText(String accountId, @NotNull String secretManagerId);

  boolean updateSecret(String accountId, String uuId, SecretText secretText);

  /**
   *  This method is called when removing application/environment, and all its referring secrets need to clear their
   *  references in usage scope to the application/environment to be deleted.
   */
  boolean updateUsageRestrictionsForSecretOrFile(
      String accountId, String uuId, UsageRestrictions usageRestrictions, boolean scopedToEntity);

  boolean deleteSecret(String accountId, String uuId);

  boolean deleteSecret(String accountId, String uuId, Map<String, String> runtimeParameters);

  boolean deleteSecretUsingUuid(String uuId);

  String saveFile(String accountId, String kmsId, String name, long fileSize, UsageRestrictions usageRestrictions,
      BoundedInputStream inputStream, boolean scopedToAccount);

  String saveFile(String accountId, String kmsId, String name, long fileSize, UsageRestrictions usageRestrictions,
      BoundedInputStream inputStream, Map<String, String> runtimeParameters, boolean scopedToAccount);

  File getFile(String accountId, String uuId, File readInto);

  byte[] getFileContents(String accountId, String uuId);

  boolean updateFile(String accountId, String name, String uuid, long fileSize, UsageRestrictions usageRestrictions,
      BoundedInputStream inputStream, boolean scopedToAccount);

  boolean updateFile(String accountId, String name, String uuid, long fileSize, UsageRestrictions usageRestrictions,
      BoundedInputStream inputStream, Map<String, String> runtimeParameters, boolean scopedToAccount);

  boolean deleteFile(String accountId, String uuId);

  boolean deleteFile(String accountId, String uuId, Map<String, String> runtimeParameters);

  PageResponse<EncryptedData> listSecrets(String accountId, PageRequest<EncryptedData> pageRequest,
      String appIdFromRequest, String envIdFromRequest, boolean details) throws IllegalAccessException;

  PageResponse<EncryptedData> listSecretsMappedToAccount(
      String accountId, PageRequest<EncryptedData> pageRequest, boolean details) throws IllegalAccessException;

  String saveSecretUsingLocalMode(String accountId, SecretText secretText);

  boolean transitionAllSecretsToHarnessSecretManager(String accountId);

  boolean canUseSecretsInAppAndEnv(
      Set<String> secretIds, String accountId, String appIdFromRequest, String envIdFromRequest);

  boolean canUseSecretsInAppAndEnv(Set<String> secretIds, String accountId, String appIdFromRequest,
      String envIdFromRequest, boolean isAccountAdmin, UsageRestrictions restrictionsFromUserPermissions,
      Map<String, Set<String>> appEnvMapFromPermissions, Map<String, List<Base>> appIdEnvMapForAccount);

  boolean hasUpdateAccessToSecrets(Set<String> secretIds, String accountId);

  void clearDefaultFlagOfSecretManagers(String accountId);

  static EncryptedRecordData buildRecordData(EncryptedData encryptedData) {
    return EncryptedRecordData.builder()
        .uuid(encryptedData.getUuid())
        .name(encryptedData.getName())
        .path(encryptedData.getPath())
        .parameters(encryptedData.getParameters())
        .encryptionKey(encryptedData.getEncryptionKey())
        .encryptedValue(encryptedData.getEncryptedValue())
        .kmsId(encryptedData.getKmsId())
        .encryptionType(encryptedData.getEncryptionType())
        .base64Encoded(encryptedData.isBase64Encoded())
        .build();
  }

  SecretManagerRuntimeParameters configureSecretManagerRuntimeCredentialsForExecution(
      String accountId, String kmsId, String executionId, Map<String, String> runtimeParameters);

  Optional<SecretManagerRuntimeParameters> getSecretManagerRuntimeCredentialsForExecution(
      String executionId, String secretManagerId);

  String saveEncryptedData(EncryptedData encryptedData);

  default void validateSecretPath(EncryptionType encryptionType, String path) {
    if (isNotEmpty(path)) {
      switch (encryptionType) {
        case VAULT:
          // Path should always have a "#" in and a key name after the #.
          if (path.indexOf('#') < 0) {
            throw new SecretManagementException(SECRET_MANAGEMENT_ERROR,
                "Secret path need to include the # sign with the the key name after. E.g. /foo/bar/my-secret#my-key.",
                USER);
          }
          break;
        case AWS_SECRETS_MANAGER:
        case CYBERARK:
        case AZURE_VAULT:
          break;
        default:
          throw new SecretManagementException(SECRET_MANAGEMENT_ERROR,
              "Secret path can be specified only if the secret manager is of VAULT/AWS_SECRETS_MANAGER/CYBERARK/AZURE_VAULT type!",
              USER);
      }
    }
  }
}
