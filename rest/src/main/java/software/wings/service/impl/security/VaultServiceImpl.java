package software.wings.service.impl.security;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;
import static software.wings.beans.ErrorCode.DEFAULT_ERROR_CODE;
import static software.wings.common.Constants.SECRET_MASK;
import static software.wings.security.encryption.SimpleEncryption.CHARSET;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.inject.Inject;

import com.mongodb.DBCursor;
import com.mongodb.DuplicateKeyException;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.MorphiaIterator;
import org.mongodb.morphia.query.Query;
import software.wings.beans.Base;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.ErrorCode;
import software.wings.beans.KmsConfig;
import software.wings.beans.VaultConfig;
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
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by rsingh on 11/2/17.
 */
public class VaultServiceImpl extends AbstractSecretServiceImpl implements VaultService {
  public static final String VAULT_VAILDATION_URL = "harness_vault_validation";
  @Inject private KmsService kmsService;

  @Override
  public EncryptedData encrypt(String name, String value, String accountId, SettingVariableTypes settingType,
      VaultConfig vaultConfig, EncryptedData encryptedData) {
    SyncTaskContext syncTaskContext = aContext().withAccountId(accountId).withAppId(Base.GLOBAL_APP_ID).build();
    try {
      return delegateProxyFactory.get(SecretManagementDelegateService.class, syncTaskContext)
          .encrypt(name, value, accountId, settingType, vaultConfig, encryptedData);
    } catch (Exception e) {
      throw new WingsException(ErrorCode.VAULT_OPERATION_ERROR).addParam("reason", e.getMessage());
    }
  }

  @Override
  public char[] decrypt(EncryptedData data, String accountId, VaultConfig vaultConfig) {
    SyncTaskContext syncTaskContext = aContext().withAccountId(accountId).withAppId(Base.GLOBAL_APP_ID).build();
    try {
      return delegateProxyFactory.get(SecretManagementDelegateService.class, syncTaskContext)
          .decrypt(data, vaultConfig);
    } catch (Exception e) {
      throw new WingsException(ErrorCode.VAULT_OPERATION_ERROR, e).addParam("reason", e.getMessage());
    }
  }

  @Override
  public VaultConfig getSecretConfig(String accountId) {
    VaultConfig vaultConfig = null;
    final MorphiaIterator<VaultConfig, VaultConfig> query = wingsPersistence.createQuery(VaultConfig.class)
                                                                .field("accountId")
                                                                .equal(accountId)
                                                                .field("isDefault")
                                                                .equal(true)
                                                                .fetch(new FindOptions().limit(1));
    try (DBCursor cursor = query.getCursor()) {
      if (query.hasNext()) {
        vaultConfig = query.next();
      }
    }

    if (vaultConfig != null) {
      EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, vaultConfig.getAuthToken());
      Preconditions.checkNotNull(encryptedData, "no encrypted record found for " + vaultConfig);

      char[] decrypt =
          kmsService.decrypt(encryptedData, accountId, kmsService.getKmsConfig(accountId, encryptedData.getKmsId()));
      vaultConfig.setAuthToken(String.valueOf(decrypt));
    }

    return vaultConfig;
  }

  @Override
  public VaultConfig getVaultConfig(String accountId, String entityId) {
    VaultConfig vaultConfig = null;
    final MorphiaIterator<VaultConfig, VaultConfig> query = wingsPersistence.createQuery(VaultConfig.class)
                                                                .field("accountId")
                                                                .equal(accountId)
                                                                .field("_id")
                                                                .equal(entityId)
                                                                .fetch(new FindOptions().limit(1));
    try (DBCursor cursor = query.getCursor()) {
      if (query.hasNext()) {
        vaultConfig = query.next();
      }
    }

    if (vaultConfig != null) {
      EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, vaultConfig.getAuthToken());
      Preconditions.checkNotNull(encryptedData, "no encrypted record found for " + vaultConfig);

      char[] decrypt =
          kmsService.decrypt(encryptedData, accountId, kmsService.getKmsConfig(accountId, encryptedData.getKmsId()));
      vaultConfig.setAuthToken(String.valueOf(decrypt));
    }

    return vaultConfig;
  }

  @Override
  public String saveVaultConfig(String accountId, VaultConfig vaultConfig) {
    try {
      validateVaultConfig(accountId, vaultConfig);
    } catch (WingsException exception) {
      throw new WingsException(ErrorCode.VAULT_OPERATION_ERROR, exception)
          .addParam("reason", "Validation failed. Please check your token");
    }

    vaultConfig.setAccountId(accountId);
    Query<VaultConfig> query = wingsPersistence.createQuery(VaultConfig.class).field("accountId").equal(accountId);
    List<VaultConfig> savedConfigs = query.asList();

    Query<KmsConfig> kmsConfigQuery = wingsPersistence.createQuery(KmsConfig.class).field("accountId").equal(accountId);
    List<KmsConfig> kmsConfigs = kmsConfigQuery.asList();

    if (savedConfigs.isEmpty() && kmsConfigs.isEmpty()) {
      vaultConfig.setDefault(true);
    }

    EncryptedData encryptedData =
        kmsService.encrypt(vaultConfig.getAuthToken().toCharArray(), accountId, kmsService.getSecretConfig(accountId));
    if (isNotBlank(vaultConfig.getUuid())) {
      EncryptedData savedEncryptedData = wingsPersistence.get(
          EncryptedData.class, wingsPersistence.get(VaultConfig.class, vaultConfig.getUuid()).getAuthToken());
      Preconditions.checkNotNull(savedEncryptedData, "reference is null for " + vaultConfig.getUuid());
      savedEncryptedData.setEncryptionKey(encryptedData.getEncryptionKey());
      savedEncryptedData.setEncryptedValue(encryptedData.getEncryptedValue());
      encryptedData = savedEncryptedData;
    }
    vaultConfig.setAuthToken(null);
    String vaultConfigId;
    try {
      vaultConfigId = wingsPersistence.save(vaultConfig);
    } catch (DuplicateKeyException e) {
      throw new WingsException(ErrorCode.VAULT_OPERATION_ERROR)
          .addParam("reason", "Another configuration with the same name exists");
    }

    encryptedData.setAccountId(accountId);
    encryptedData.addParent(vaultConfigId);
    encryptedData.setType(SettingVariableTypes.VAULT);
    encryptedData.setName(vaultConfig.getName() + "_token");
    String encryptedDataId = wingsPersistence.save(encryptedData);

    vaultConfig.setAuthToken(encryptedDataId);
    wingsPersistence.save(vaultConfig);

    if (vaultConfig.isDefault() && (!savedConfigs.isEmpty() || !kmsConfigs.isEmpty())) {
      for (VaultConfig savedConfig : savedConfigs) {
        if (vaultConfig.getUuid().equals(savedConfig.getUuid())) {
          continue;
        }
        if (savedConfig.isDefault()) {
          savedConfig.setDefault(false);
          wingsPersistence.save(savedConfig);
        }
      }

      for (KmsConfig kmsConfig : kmsConfigs) {
        if (kmsConfig.isDefault()) {
          kmsConfig.setDefault(false);
          wingsPersistence.save(kmsConfig);
        }
      }
    }

    return vaultConfigId;
  }

  @Override
  public boolean deleteVaultConfig(String accountId, String vaultConfigId) {
    final MorphiaIterator<EncryptedData, EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class)
                                                                    .field("accountId")
                                                                    .equal(accountId)
                                                                    .field("kmsId")
                                                                    .equal(vaultConfigId)
                                                                    .field("encryptionType")
                                                                    .equal(EncryptionType.VAULT)
                                                                    .fetch(new FindOptions().limit(1));

    try (DBCursor cursor = query.getCursor()) {
      if (query.hasNext()) {
        String message = "Can not delete the vault configuration since there are secrets encrypted with this. "
            + "Please transition your secrets to a new kms and then try again";
        throw new WingsException(ErrorCode.VAULT_OPERATION_ERROR).addParam("reason", message);
      }
    }

    VaultConfig vaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
    Preconditions.checkNotNull(vaultConfig, "no vault config found with id " + vaultConfigId);

    wingsPersistence.delete(EncryptedData.class, vaultConfig.getAuthToken());
    return wingsPersistence.delete(vaultConfig);
  }

  @Override
  public Collection<VaultConfig> listVaultConfigs(String accountId, boolean maskSecret) {
    List<VaultConfig> rv = new ArrayList<>();

    final MorphiaIterator<VaultConfig, VaultConfig> query =
        wingsPersistence.createQuery(VaultConfig.class).field("accountId").equal(accountId).order("-createdAt").fetch();

    try (DBCursor cursor = query.getCursor()) {
      while (query.hasNext()) {
        VaultConfig vaultConfig = query.next();
        Query<EncryptedData> encryptedDataQuery =
            wingsPersistence.createQuery(EncryptedData.class).field("kmsId").equal(vaultConfig.getUuid());
        vaultConfig.setNumOfEncryptedValue(encryptedDataQuery.asKeyList().size());
        if (maskSecret) {
          vaultConfig.setAuthToken(SECRET_MASK);
        } else {
          EncryptedData tokenData = wingsPersistence.get(EncryptedData.class, vaultConfig.getAuthToken());
          Preconditions.checkNotNull(tokenData, "token data null for " + vaultConfig);
          char[] decryptedToken =
              kmsService.decrypt(tokenData, accountId, kmsService.getKmsConfig(accountId, tokenData.getKmsId()));
          vaultConfig.setAuthToken(String.valueOf(decryptedToken));
        }
        vaultConfig.setEncryptionType(EncryptionType.VAULT);
        rv.add(vaultConfig);
      }
    }
    return rv;
  }

  @Override
  public EncryptedData encryptFile(
      String accountId, String name, BoundedInputStream inputStream, EncryptedData savedEncryptedData) {
    try {
      VaultConfig vaultConfig = getSecretConfig(accountId);
      Preconditions.checkNotNull(vaultConfig);
      byte[] bytes = ByteStreams.toByteArray(inputStream);
      EncryptedData fileData = encrypt(name, new String(CHARSET.decode(ByteBuffer.wrap(bytes)).array()), accountId,
          SettingVariableTypes.CONFIG_FILE, vaultConfig, savedEncryptedData);
      fileData.setAccountId(accountId);
      fileData.setName(name);
      fileData.setType(SettingVariableTypes.CONFIG_FILE);
      fileData.setFileSize(inputStream.getTotalBytesRead());
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
      Files.write(CHARSET.encode(CharBuffer.wrap(decrypt)).array(), file);
      return file;
    } catch (IOException ioe) {
      throw new WingsException(DEFAULT_ERROR_CODE, ioe);
    }
  }

  @Override
  public void decryptToStream(String accountId, EncryptedData encryptedData, OutputStream output) {
    try {
      VaultConfig vaultConfig = getSecretConfig(accountId);
      Preconditions.checkNotNull(vaultConfig);
      Preconditions.checkNotNull(encryptedData);
      char[] decrypt = decrypt(encryptedData, accountId, vaultConfig);
      output.write(CHARSET.encode(CharBuffer.wrap(decrypt)).array(), 0, decrypt.length);
      output.flush();
    } catch (IOException ioe) {
      throw new WingsException(DEFAULT_ERROR_CODE, ioe);
    }
  }

  @Override
  public void deleteSecret(String accountId, String path, VaultConfig vaultConfig) {
    SyncTaskContext syncTaskContext = aContext().withAccountId(accountId).withAppId(Base.GLOBAL_APP_ID).build();
    try {
      delegateProxyFactory.get(SecretManagementDelegateService.class, syncTaskContext)
          .deleteVaultSecret(path, vaultConfig);
    } catch (Exception e) {
      throw new WingsException(ErrorCode.VAULT_OPERATION_ERROR).addParam("reason", e.getMessage());
    }
  }

  void validateVaultConfig(String accountId, VaultConfig vaultConfig) {
    encrypt(VAULT_VAILDATION_URL, VAULT_VAILDATION_URL, accountId, SettingVariableTypes.VAULT, vaultConfig, null);
  }
}
