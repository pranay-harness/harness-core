package software.wings.service.impl.security;

import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;
import static software.wings.beans.ErrorCode.DEFAULT_ERROR_CODE;
import static software.wings.helpers.ext.vault.VaultRestClient.BASE_VAULT_URL;
import static software.wings.service.impl.security.KmsServiceImpl.SECRET_MASK;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import software.wings.api.KmsTransitionEvent;
import software.wings.beans.Base;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.ErrorCode;
import software.wings.beans.KmsConfig;
import software.wings.beans.VaultConfig;
import software.wings.core.queue.Queue;
import software.wings.exception.WingsException;
import software.wings.security.EncryptionType;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.SecretManagementDelegateService;
import software.wings.service.intfc.security.VaultService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.BoundedInputStream;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;

/**
 * Created by rsingh on 11/2/17.
 */
public class VaultServiceImpl extends AbstractSecretServiceImpl implements VaultService {
  @Inject private Queue<KmsTransitionEvent> transitionKmsQueue;
  @Inject private KmsService kmsService;

  @Override
  public EncryptedData encrypt(String name, String value, String accountId, SettingVariableTypes settingType,
      VaultConfig vaultConfig, EncryptedData encryptedData) {
    SyncTaskContext syncTaskContext = aContext().withAccountId(accountId).withAppId(Base.GLOBAL_APP_ID).build();
    try {
      return delegateProxyFactory.get(SecretManagementDelegateService.class, syncTaskContext)
          .encrypt(name, value, accountId, settingType, vaultConfig, encryptedData);
    } catch (Exception e) {
      throw new WingsException(ErrorCode.VAULT_OPERATION_ERROR, "reason", e.getMessage());
    }
  }

  @Override
  public char[] decrypt(EncryptedData data, String accountId, VaultConfig vaultConfig) {
    SyncTaskContext syncTaskContext = aContext().withAccountId(accountId).withAppId(Base.GLOBAL_APP_ID).build();
    try {
      return delegateProxyFactory.get(SecretManagementDelegateService.class, syncTaskContext)
          .decrypt(data, vaultConfig);
    } catch (Exception e) {
      throw new WingsException(ErrorCode.VAULT_OPERATION_ERROR, "reason", e.getMessage());
    }
  }

  @Override
  public VaultConfig getSecretConfig(String accountId) {
    VaultConfig vaultConfig = null;
    Iterator<VaultConfig> query = wingsPersistence.createQuery(VaultConfig.class)
                                      .field("accountId")
                                      .equal(accountId)
                                      .field("isDefault")
                                      .equal(true)
                                      .fetch(new FindOptions().limit(1));
    if (query.hasNext()) {
      vaultConfig = query.next();
    }

    if (vaultConfig != null) {
      EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, vaultConfig.getAuthToken());
      Preconditions.checkNotNull(encryptedData, "no encrypted record found for " + vaultConfig);

      char[] decrypt = kmsService.decrypt(encryptedData, accountId, kmsService.getSecretConfig(accountId));
      vaultConfig.setAuthToken(String.valueOf(decrypt));
    }

    return vaultConfig;
  }

  @Override
  public VaultConfig getVaultConfig(String accountId, String entityId) {
    VaultConfig vaultConfig = null;
    Iterator<VaultConfig> query = wingsPersistence.createQuery(VaultConfig.class)
                                      .field("accountId")
                                      .equal(accountId)
                                      .field("_id")
                                      .equal(entityId)
                                      .fetch(new FindOptions().limit(1));
    if (query.hasNext()) {
      vaultConfig = query.next();
    }

    if (vaultConfig != null) {
      EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, vaultConfig.getAuthToken());
      Preconditions.checkNotNull(encryptedData, "no encrypted record found for " + vaultConfig);

      char[] decrypt = kmsService.decrypt(encryptedData, accountId, kmsService.getSecretConfig(accountId));
      vaultConfig.setAuthToken(String.valueOf(decrypt));
    }

    return vaultConfig;
  }

  @Override
  public boolean saveVaultConfig(String accountId, VaultConfig vaultConfig) {
    try {
      validateVaultConfig(accountId, vaultConfig);
    } catch (WingsException e) {
      throw new WingsException(ErrorCode.VAULT_OPERATION_ERROR, "reason", "Validation failed. Please check your token");
    }

    vaultConfig.setAccountId(accountId);
    Query<VaultConfig> query = wingsPersistence.createQuery(VaultConfig.class).field("accountId").equal(accountId);
    Collection<VaultConfig> savedConfigs = query.asList();

    if (savedConfigs.isEmpty()) {
      vaultConfig.setDefault(true);
    }

    EncryptedData encryptedData =
        kmsService.encrypt(vaultConfig.getAuthToken().toCharArray(), accountId, kmsService.getSecretConfig(accountId));
    encryptedData.setAccountId(accountId);
    String encryptedDataId = wingsPersistence.save(encryptedData);
    vaultConfig.setAuthToken(encryptedDataId);
    String vaultConfigId = wingsPersistence.save(vaultConfig);
    encryptedData.setParentId(vaultConfigId);
    encryptedData.setType(SettingVariableTypes.VAULT);
    wingsPersistence.save(encryptedData);

    if (vaultConfig.isDefault() && !savedConfigs.isEmpty()) {
      for (VaultConfig savedConfig : savedConfigs) {
        if (vaultConfig.getUuid().equals(savedConfig.getUuid())) {
          continue;
        }
        savedConfig.setDefault(false);
        wingsPersistence.save(savedConfig);
      }
    }

    return true;
  }

  @Override
  public boolean deleteVaultConfig(String accountId, String vaultConfigId) {
    Iterator<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class)
                                        .field("accountId")
                                        .equal(accountId)
                                        .field("kmsId")
                                        .equal(vaultConfigId)
                                        .field("encryptionType")
                                        .equal(EncryptionType.VAULT)
                                        .fetch(new FindOptions().limit(1));

    if (query.hasNext()) {
      String message = "Can not delete the vault configuration since there are secrets encrypted with this. "
          + "Please transition your secrets to a new kms and then try again";
      throw new WingsException(ErrorCode.VAULT_OPERATION_ERROR, "reason", message);
    }

    VaultConfig vaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
    Preconditions.checkNotNull(vaultConfig, "no vault config found with id " + vaultConfigId);

    wingsPersistence.delete(EncryptedData.class, vaultConfig.getAuthToken());
    return wingsPersistence.delete(vaultConfig);
  }

  @Override
  public Collection<VaultConfig> listVaultConfigs(String accountId) {
    List<VaultConfig> rv = new ArrayList<>();
    Iterator<VaultConfig> query =
        wingsPersistence.createQuery(VaultConfig.class).field("accountId").equal(accountId).order("-createdAt").fetch();

    while (query.hasNext()) {
      VaultConfig vaultConfig = query.next();
      Query<EncryptedData> encryptedDataQuery =
          wingsPersistence.createQuery(EncryptedData.class).field("kmsId").equal(vaultConfig.getUuid());
      vaultConfig.setNumOfEncryptedValue(encryptedDataQuery.asKeyList().size());
      vaultConfig.setAuthToken(SECRET_MASK);
      rv.add(vaultConfig);
    }
    return rv;
  }

  @Override
  public boolean transitionVault(String accountId, String fromVaultId, String toVaultId) {
    return transitionSecretStore(accountId, fromVaultId, toVaultId, EncryptionType.VAULT);
  }

  @Override
  public void changeVault(String accountId, String entityId, String fromVaultId, String toVaultId) {
    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, entityId);
    Preconditions.checkNotNull(encryptedData, "No encrypted data with id " + entityId);
    VaultConfig fromConfig = getVaultConfig(accountId, fromVaultId);
    Preconditions.checkNotNull(fromConfig, "No kms found for account " + accountId + " with id " + entityId);
    VaultConfig toConfig = getVaultConfig(accountId, toVaultId);
    Preconditions.checkNotNull(toConfig, "No kms found for account " + accountId + " with id " + entityId);

    char[] decrypted = decrypt(encryptedData, accountId, fromConfig);

    String encryptionKey = encryptedData.getEncryptionKey();

    String keyString = encryptionKey.substring(encryptionKey.indexOf(BASE_VAULT_URL) + BASE_VAULT_URL.length());
    String[] split = keyString.split("/");
    SettingVariableTypes settingVariableType = SettingVariableTypes.valueOf(split[1]);
    String keyName = split[2];
    EncryptedData encrypted = encrypt(keyName, String.valueOf(decrypted), accountId, settingVariableType, toConfig,
        EncryptedData.builder().encryptionKey(encryptionKey).build());
    encryptedData.setKmsId(toVaultId);
    encryptedData.setEncryptionKey(encrypted.getEncryptionKey());
    encryptedData.setEncryptedValue(encrypted.getEncryptedValue());

    wingsPersistence.save(encryptedData);
  }

  @Override
  public EncryptedData encryptFile(BoundedInputStream inputStream, String accountId, String uuid) {
    try {
      VaultConfig vaultConfig = getSecretConfig(accountId);
      Preconditions.checkNotNull(vaultConfig);
      byte[] bytes = ByteStreams.toByteArray(inputStream);
      EncryptedData fileData =
          encrypt("FILE", new String(bytes), accountId, SettingVariableTypes.CONFIG_FILE, vaultConfig, null);
      fileData.setAccountId(accountId);
      fileData.setType(SettingVariableTypes.CONFIG_FILE);
      fileData.setUuid(uuid);
      wingsPersistence.save(fileData);
      return fileData;
    } catch (IOException ioe) {
      throw new WingsException(DEFAULT_ERROR_CODE, ioe);
    }
  }

  @Override
  public File decryptFile(File file, String accountId, EncryptedData encryptedData) {
    try {
      VaultConfig vaultConfig = getSecretConfig(accountId);
      Preconditions.checkNotNull(vaultConfig);
      Preconditions.checkNotNull(encryptedData);
      char[] decrypt = decrypt(encryptedData, accountId, vaultConfig);
      Files.write(new String(decrypt).getBytes(), file);
      return file;
    } catch (IOException ioe) {
      throw new WingsException(DEFAULT_ERROR_CODE, ioe);
    }
  }

  private void validateVaultConfig(String accountId, VaultConfig vaultConfig) {
    encrypt(UUID.randomUUID().toString(), UUID.randomUUID().toString(), accountId, SettingVariableTypes.VAULT,
        vaultConfig, null);
  }
}
