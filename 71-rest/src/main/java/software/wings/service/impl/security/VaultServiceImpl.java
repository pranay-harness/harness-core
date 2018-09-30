package software.wings.service.impl.security;

import static io.harness.data.encoding.EncodingUtils.decodeBase64;
import static io.harness.data.encoding.EncodingUtils.encodeBase64ToByteArray;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.eraro.ErrorCode.DEFAULT_ERROR_CODE;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_SRE;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;
import static software.wings.common.Constants.SECRET_MASK;
import static software.wings.security.encryption.SimpleEncryption.CHARSET;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.inject.Inject;

import com.mongodb.DuplicateKeyException;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.persistence.HIterator;
import org.mongodb.morphia.query.CountOptions;
import org.mongodb.morphia.query.Query;
import software.wings.beans.Base;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.KmsConfig;
import software.wings.beans.VaultConfig;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.KmsSetupAlert;
import software.wings.common.Constants;
import software.wings.security.EncryptionType;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.intfc.AlertService;
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
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by rsingh on 11/2/17.
 */
public class VaultServiceImpl extends AbstractSecretServiceImpl implements VaultService {
  public static final String VAULT_VAILDATION_URL = "harness_vault_validation";
  @Inject private KmsService kmsService;
  @Inject private AlertService alertService;

  @Override
  public EncryptedData encrypt(String name, String value, String accountId, SettingVariableTypes settingType,
      VaultConfig vaultConfig, EncryptedData encryptedData) {
    SyncTaskContext syncTaskContext = aContext().withAccountId(accountId).withAppId(Base.GLOBAL_APP_ID).build();
    return delegateProxyFactory.get(SecretManagementDelegateService.class, syncTaskContext)
        .encrypt(name, value, accountId, settingType, vaultConfig, encryptedData);
  }

  @Override
  public char[] decrypt(EncryptedData data, String accountId, VaultConfig vaultConfig) {
    SyncTaskContext syncTaskContext = aContext().withAccountId(accountId).withAppId(Base.GLOBAL_APP_ID).build();
    return delegateProxyFactory.get(SecretManagementDelegateService.class, syncTaskContext).decrypt(data, vaultConfig);
  }

  @Override
  public VaultConfig getSecretConfig(String accountId) {
    VaultConfig vaultConfig =
        wingsPersistence.createQuery(VaultConfig.class).filter("accountId", accountId).filter("isDefault", true).get();

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
    VaultConfig vaultConfig =
        wingsPersistence.createQuery(VaultConfig.class).filter("accountId", accountId).filter("_id", entityId).get();

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
  public void renewTokens(String accountId) {
    long currentTime = System.currentTimeMillis();
    logger.info("renewing vault token for {}", accountId);
    try (HIterator<VaultConfig> query =
             new HIterator<>(wingsPersistence.createQuery(VaultConfig.class).filter("accountId", accountId).fetch())) {
      while (query.hasNext()) {
        VaultConfig vaultConfig = query.next();
        // don't renew if renewal interval not configured
        if (vaultConfig.getRenewIntervalHours() <= 0) {
          logger.info("renewing not configured for {} for account {}", vaultConfig.getUuid(), accountId);
          continue;
        }
        // don't renew if renewed within configured time
        if (TimeUnit.MILLISECONDS.toHours(currentTime - vaultConfig.getRenewedAt())
            < vaultConfig.getRenewIntervalHours()) {
          logger.info("{} renewed at {} not renewing now for account {}", vaultConfig.getUuid(),
              new Date(vaultConfig.getRenewedAt()), accountId);
          continue;
        }

        VaultConfig decryptedVaultConfig = getVaultConfig(accountId, vaultConfig.getUuid());
        SyncTaskContext syncTaskContext = aContext().withAccountId(accountId).withAppId(Base.GLOBAL_APP_ID).build();
        KmsSetupAlert kmsSetupAlert =
            KmsSetupAlert.builder()
                .kmsId(vaultConfig.getUuid())
                .message(vaultConfig.getName()
                    + "(Hashicorp Vault) is not able to renew the token. Please check your setup and ensure that token is renewable")
                .build();
        try {
          delegateProxyFactory.get(SecretManagementDelegateService.class, syncTaskContext)
              .renewVaultToken(decryptedVaultConfig);
          alertService.closeAlert(accountId, Base.GLOBAL_APP_ID, AlertType.InvalidKMS, kmsSetupAlert);
          vaultConfig.setRenewedAt(System.currentTimeMillis());
          wingsPersistence.save(vaultConfig);
        } catch (Exception e) {
          logger.info("Error while renewing token for : " + vaultConfig, e);
          alertService.openAlert(accountId, Base.GLOBAL_APP_ID, AlertType.InvalidKMS, kmsSetupAlert);
        }
      }
    }
  }

  @Override
  public String saveVaultConfig(String accountId, VaultConfig vaultConfig) {
    boolean shouldVerify = true;
    if (!isEmpty(vaultConfig.getUuid())) {
      VaultConfig savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfig.getUuid());
      shouldVerify = !savedVaultConfig.getVaultUrl().equals(vaultConfig.getVaultUrl())
          || !Constants.SECRET_MASK.equals(vaultConfig.getAuthToken());
    }
    if (shouldVerify) {
      validateVaultConfig(accountId, vaultConfig);
    }

    // update without token or url changes
    if (!shouldVerify) {
      VaultConfig savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfig.getUuid());
      savedVaultConfig.setName(vaultConfig.getName());
      savedVaultConfig.setRenewIntervalHours(vaultConfig.getRenewIntervalHours());
      savedVaultConfig.setDefault(vaultConfig.isDefault());
      return wingsPersistence.save(savedVaultConfig);
    }

    vaultConfig.setAccountId(accountId);
    Query<VaultConfig> query = wingsPersistence.createQuery(VaultConfig.class).filter("accountId", accountId);
    List<VaultConfig> savedConfigs = query.asList();

    Query<KmsConfig> kmsConfigQuery = wingsPersistence.createQuery(KmsConfig.class).filter("accountId", accountId);
    List<KmsConfig> kmsConfigs = kmsConfigQuery.asList();

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
      throw new WingsException(
          ErrorCode.VAULT_OPERATION_ERROR, "Another configuration with the same name exists", USER_SRE)
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
    final long count = wingsPersistence.createQuery(EncryptedData.class)
                           .filter("accountId", accountId)
                           .filter("kmsId", vaultConfigId)
                           .filter("encryptionType", EncryptionType.VAULT)
                           .count(new CountOptions().limit(1));

    if (count > 0) {
      String message = "Can not delete the vault configuration since there are secrets encrypted with this. "
          + "Please transition your secrets to a new kms and then try again";
      throw new WingsException(ErrorCode.VAULT_OPERATION_ERROR).addParam("reason", message);
    }

    VaultConfig vaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
    Preconditions.checkNotNull(vaultConfig, "no vault config found with id " + vaultConfigId);

    wingsPersistence.delete(EncryptedData.class, vaultConfig.getAuthToken());
    return wingsPersistence.delete(vaultConfig);
  }

  @Override
  public Collection<VaultConfig> listVaultConfigs(String accountId, boolean maskSecret) {
    List<VaultConfig> rv = new ArrayList<>();
    try (HIterator<VaultConfig> query = new HIterator<>(wingsPersistence.createQuery(VaultConfig.class)
                                                            .filter("accountId", accountId)
                                                            .order("-createdAt")
                                                            .fetch())) {
      while (query.hasNext()) {
        VaultConfig vaultConfig = query.next();
        Query<EncryptedData> encryptedDataQuery = wingsPersistence.createQuery(EncryptedData.class)
                                                      .filter("kmsId", vaultConfig.getUuid())
                                                      .filter("accountId", accountId);
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
  public EncryptedData encryptFile(String accountId, VaultConfig vaultConfig, String name,
      BoundedInputStream inputStream, EncryptedData savedEncryptedData) {
    try {
      Preconditions.checkNotNull(vaultConfig);
      byte[] bytes = encodeBase64ToByteArray(ByteStreams.toByteArray(inputStream));
      EncryptedData fileData = encrypt(name, new String(CHARSET.decode(ByteBuffer.wrap(bytes)).array()), accountId,
          SettingVariableTypes.CONFIG_FILE, vaultConfig, savedEncryptedData);
      fileData.setAccountId(accountId);
      fileData.setName(name);
      fileData.setType(SettingVariableTypes.CONFIG_FILE);
      fileData.setBase64Encoded(true);
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
      byte[] fileData =
          encryptedData.isBase64Encoded() ? decodeBase64(decrypt) : CHARSET.encode(CharBuffer.wrap(decrypt)).array();
      Files.write(fileData, file);
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
      byte[] fileData =
          encryptedData.isBase64Encoded() ? decodeBase64(decrypt) : CHARSET.encode(CharBuffer.wrap(decrypt)).array();
      output.write(fileData, 0, fileData.length);
      output.flush();
    } catch (IOException ioe) {
      throw new WingsException(DEFAULT_ERROR_CODE, ioe);
    }
  }

  @Override
  public void deleteSecret(String accountId, String path, VaultConfig vaultConfig) {
    SyncTaskContext syncTaskContext = aContext().withAccountId(accountId).withAppId(Base.GLOBAL_APP_ID).build();
    delegateProxyFactory.get(SecretManagementDelegateService.class, syncTaskContext)
        .deleteVaultSecret(path, vaultConfig);
  }

  void validateVaultConfig(String accountId, VaultConfig vaultConfig) {
    try {
      encrypt(VAULT_VAILDATION_URL, VAULT_VAILDATION_URL, accountId, SettingVariableTypes.VAULT, vaultConfig, null);
    } catch (WingsException e) {
      String message =
          "Was not able to reach vault using given credentials. Please check your credentials and try again";
      throw new WingsException(ErrorCode.VAULT_OPERATION_ERROR, message, USER, e).addParam("reason", message);
    }
  }
}
