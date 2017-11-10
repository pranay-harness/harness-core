package software.wings.service.intfc.security;

import software.wings.annotation.Encryptable;
import software.wings.beans.UuidAware;
import software.wings.security.EncryptionType;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.security.encryption.SecretChangeLog;
import software.wings.security.encryption.SecretUsageLog;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.BoundedInputStream;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;

/**
 * Created by rsingh on 10/30/17.
 */
public interface SecretManager {
  List<EncryptionConfig> listEncryptionConfig(String accountId);

  EncryptionType getEncryptionType(String accountId);

  List<SecretUsageLog> getUsageLogs(final String entityId, SettingValue.SettingVariableTypes variableType)
      throws IllegalAccessException;

  List<SecretChangeLog> getChangeLogs(String entityId, SettingVariableTypes variableType) throws IllegalAccessException;

  EncryptedData encrypt(EncryptionType encryptionType, String accountId, SettingVariableTypes settingType,
      char[] secret, Field decryptedField, EncryptedData encryptedData);

  List<EncryptedDataDetail> getEncryptionDetails(Encryptable object, String appId, String workflowExecutionId);

  Collection<UuidAware> listEncryptedValues(String accountId);

  EncryptedData encryptFile(BoundedInputStream inputStream, String accountId, String uuid);

  File decryptFile(File file, String accountId, EncryptedData encryptedData);

  String getEncryptedYamlRef(Encryptable object, String... fieldName) throws IllegalAccessException;

  EncryptedData getEncryptedDataFromYamlRef(String encryptedYamlRef) throws IllegalAccessException;

  boolean transitionSecrets(String accountId, String fromVaultId, String toVaultId, EncryptionType encryptionType);
}
