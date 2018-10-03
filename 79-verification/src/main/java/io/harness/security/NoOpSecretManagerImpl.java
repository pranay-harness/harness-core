package io.harness.security;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.persistence.UuidAware;
import software.wings.annotation.Encryptable;
import software.wings.security.EncryptionType;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.security.encryption.SecretChangeLog;
import software.wings.security.encryption.SecretUsageLog;
import software.wings.service.intfc.security.EncryptionConfig;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.settings.UsageRestrictions;
import software.wings.utils.BoundedInputStream;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Created by rsingh on 9/7/18.
 */
public class NoOpSecretManagerImpl implements SecretManager {
  @Override
  public List<EncryptionConfig> listEncryptionConfig(String accountId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public EncryptionType getEncryptionType(String accountId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void maskEncryptedFields(Encryptable object) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void resetUnchangedEncryptedFields(Encryptable sourceObject, Encryptable destinationObject) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PageResponse<SecretUsageLog> getUsageLogs(PageRequest<SecretUsageLog> pageRequest, String accountId,
      String entityId, SettingVariableTypes variableType) throws IllegalAccessException {
    throw new UnsupportedOperationException();
  }

  @Override
  public long getUsageLogsSize(String entityId, SettingVariableTypes variableType) throws IllegalAccessException {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<SecretChangeLog> getChangeLogs(String accountId, String entityId, SettingVariableTypes variableType)
      throws IllegalAccessException {
    throw new UnsupportedOperationException();
  }

  @Override
  public String encrypt(String accountId, String secret, UsageRestrictions usageRestrictions) {
    throw new UnsupportedOperationException();
  }

  @Override
  public EncryptedData encrypt(EncryptionType encryptionType, String accountId, SettingVariableTypes settingType,
      char[] secret, EncryptedData encryptedData, String secretName, UsageRestrictions usageRestrictions) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Optional<EncryptedDataDetail> encryptedDataDetails(String accountId, String fieldName, String refId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<EncryptedDataDetail> getEncryptionDetails(Encryptable object, String appId, String workflowExecutionId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public EncryptionConfig getEncryptionConfig(String accountId, String entityId, EncryptionType encryptionType) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<UuidAware> listEncryptedValues(String accountId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PageResponse<UuidAware> listEncryptedValues(String accountId, PageRequest<EncryptedData> pageRequest) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getEncryptedYamlRef(Encryptable object, String... fieldName) throws IllegalAccessException {
    throw new UnsupportedOperationException();
  }

  @Override
  public EncryptedData getEncryptedDataFromYamlRef(String encryptedYamlRef) throws IllegalAccessException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean transitionSecrets(String accountId, EncryptionType fromEncryptionType, String fromSecretId,
      EncryptionType toEncryptionType, String toSecretId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void changeSecretManager(String accountId, String entityId, EncryptionType fromEncryptionType,
      String fromKmsId, EncryptionType toEncryptionType, String toKmsId) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void checkAndAlertForInvalidManagers() {
    throw new UnsupportedOperationException();
  }

  @Override
  public EncryptedData getEncryptedDataByName(String accountId, String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String saveSecret(String accountId, String name, String value, UsageRestrictions usageRestrictions) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean updateSecret(
      String accountId, String uuId, String name, String value, UsageRestrictions usageRestrictions) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean updateUsageRestrictionsForSecretOrFile(
      String accountId, String uuId, UsageRestrictions usageRestrictions) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean deleteSecret(String accountId, String uuId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean deleteSecretUsingUuid(String uuId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String saveFile(
      String accountId, String name, UsageRestrictions usageRestrictions, BoundedInputStream inputStream) {
    throw new UnsupportedOperationException();
  }

  @Override
  public File getFile(String accountId, String uuId, File readInto) {
    throw new UnsupportedOperationException();
  }

  @Override
  public byte[] getFileContents(String accountId, String uuId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean updateFile(
      String accountId, String name, String uuid, UsageRestrictions usageRestrictions, BoundedInputStream inputStream) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean deleteFile(String accountId, String uuId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PageResponse<EncryptedData> listSecrets(String accountId, PageRequest<EncryptedData> pageRequest,
      String appIdFromRequest, String envIdFromRequest, boolean details) throws IllegalAccessException {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<UuidAware> getSecretUsage(String accountId, String secretTextId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String saveSecretUsingLocalMode(
      String accountId, String name, String value, UsageRestrictions usageRestrictions) {
    throw new UnsupportedOperationException();
  }
}
