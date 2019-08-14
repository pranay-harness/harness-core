package software.wings.service.impl.security;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.CYBERARK_OPERATION_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.service.intfc.security.SecretManagementDelegateService.NUM_OF_RETRIES;
import static software.wings.service.intfc.security.SecretManager.ACCOUNT_ID_KEY;
import static software.wings.service.intfc.security.SecretManager.SECRET_NAME_KEY;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.mongodb.DuplicateKeyException;
import io.harness.exception.WingsException;
import io.harness.network.Http;
import io.harness.security.encryption.EncryptionType;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.CountOptions;
import org.mongodb.morphia.query.Query;
import software.wings.beans.CyberArkConfig;
import software.wings.beans.SecretManagerConfig;
import software.wings.beans.SyncTaskContext;
import software.wings.features.SecretsManagementFeature;
import software.wings.features.api.PremiumFeature;
import software.wings.helpers.ext.cyberark.CyberArkRestClientFactory;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedData.EncryptedDataKeys;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.security.CyberArkService;
import software.wings.service.intfc.security.SecretManagementDelegateService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.time.Duration;
import java.util.Objects;

/**
 * @author marklu on 2019-08-01
 */
@Singleton
@Slf4j
public class CyberArkServiceImpl extends AbstractSecretServiceImpl implements CyberArkService {
  private static final String CLIENT_CERTIFICATE_NAME_SUFFIX = "_clientCertificate";

  @Inject private AlertService alertService;
  @Inject private AccountService accountService;
  @Inject @Named(SecretsManagementFeature.FEATURE_NAME) private PremiumFeature secretsManagementFeature;

  @Override
  public char[] decrypt(EncryptedData data, String accountId, CyberArkConfig cyberArkConfig) {
    int failedAttempts = 0;
    while (true) {
      try {
        SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                              .accountId(accountId)
                                              .timeout(Duration.ofSeconds(5).toMillis())
                                              .appId(GLOBAL_APP_ID)
                                              .correlationId(data.getName())
                                              .build();
        return delegateProxyFactory.get(SecretManagementDelegateService.class, syncTaskContext)
            .decrypt(data, cyberArkConfig);
      } catch (WingsException e) {
        failedAttempts++;
        logger.info("AWS Secrets Manager decryption failed for encryptedData {}. trial num: {}", data.getName(),
            failedAttempts, e);
        if (failedAttempts == NUM_OF_RETRIES) {
          throw e;
        }
        sleep(ofMillis(1000));
      }
    }
  }

  @Override
  public CyberArkConfig getConfig(String accountId, String configId) {
    CyberArkConfig cyberArkConfig = wingsPersistence.createQuery(CyberArkConfig.class)
                                        .filter(ACCOUNT_ID_KEY, accountId)
                                        .filter(ID_KEY, configId)
                                        .get();
    if (cyberArkConfig != null) {
      decryptCyberArkConfigSecrets(accountId, cyberArkConfig, false);
    }

    return cyberArkConfig;
  }

  @Override
  public String saveConfig(String accountId, CyberArkConfig cyberArkConfig) {
    cyberArkConfig.setAccountId(accountId);
    checkIfSecretsManagerConfigCanBeCreatedOrUpdated(accountId);

    CyberArkConfig savedConfig = null;
    boolean shouldVerify = true;
    if (!isEmpty(cyberArkConfig.getUuid())) {
      savedConfig = wingsPersistence.get(CyberArkConfig.class, cyberArkConfig.getUuid());
      shouldVerify = (!SECRET_MASK.equals(cyberArkConfig.getClientCertificate())
                         && !Objects.equals(cyberArkConfig.getClientCertificate(), savedConfig.getClientCertificate()))
          || !Objects.equals(cyberArkConfig.getCyberArkUrl(), savedConfig.getCyberArkUrl())
          || !Objects.equals(cyberArkConfig.getAppId(), savedConfig.getAppId());
    }
    if (shouldVerify) {
      // New CyberArk Secret Manager configuration, need to validate it's parameters
      validateConfig(cyberArkConfig);
    } else {
      // update without client certificate or url changes
      savedConfig.setName(cyberArkConfig.getName());
      savedConfig.setDefault(cyberArkConfig.isDefault());
      return secretManagerConfigService.save(savedConfig);
    }

    EncryptedData clientCertEncryptedData =
        getEncryptedDataForClientCertificateField(savedConfig, cyberArkConfig, cyberArkConfig.getClientCertificate());

    cyberArkConfig.setClientCertificate(null);
    String secretsManagerConfigId;
    try {
      secretsManagerConfigId = secretManagerConfigService.save(cyberArkConfig);
    } catch (DuplicateKeyException e) {
      throw new WingsException(CYBERARK_OPERATION_ERROR, USER_SRE)
          .addParam(REASON_KEY, "Another CyberArk secret manager with the same name or URL exists");
    }

    // Create a LOCAL encrypted record for AWS secret key
    String clientCertEncryptedDataId = saveClientCertificateField(
        cyberArkConfig, secretsManagerConfigId, clientCertEncryptedData, CLIENT_CERTIFICATE_NAME_SUFFIX);
    cyberArkConfig.setClientCertificate(clientCertEncryptedDataId);

    return secretManagerConfigService.save(cyberArkConfig);
  }

  private EncryptedData getEncryptedDataForClientCertificateField(
      CyberArkConfig savedConfig, CyberArkConfig cyberArkConfig, String clientCertificate) {
    EncryptedData encryptedData = isNotEmpty(clientCertificate) && !Objects.equals(SECRET_MASK, clientCertificate)
        ? encryptLocal(clientCertificate.toCharArray())
        : null;

    EncryptedData savedEncryptedData = null;
    if (savedConfig != null) {
      // Get by auth token encrypted record by Id or name.
      Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class);
      query.criteria(ACCOUNT_ID_KEY)
          .equal(cyberArkConfig.getAccountId())
          .or(query.criteria(ID_KEY).equal(savedConfig.getClientCertificate()),
              query.criteria(SECRET_NAME_KEY).equal(cyberArkConfig.getName() + CLIENT_CERTIFICATE_NAME_SUFFIX));
      savedEncryptedData = query.get();
    }
    if (savedEncryptedData != null && encryptedData != null) {
      savedEncryptedData.setEncryptionKey(encryptedData.getEncryptionKey());
      savedEncryptedData.setEncryptedValue(encryptedData.getEncryptedValue());
      return savedEncryptedData;
    } else {
      return encryptedData;
    }
  }

  private String saveClientCertificateField(CyberArkConfig cyberArkConfig, String configId,
      EncryptedData secretFieldEncryptedData, String clientCertNameSuffix) {
    String secretFieldEncryptedDataId = null;
    if (secretFieldEncryptedData != null) {
      secretFieldEncryptedData.setAccountId(cyberArkConfig.getAccountId());
      secretFieldEncryptedData.addParent(configId);
      secretFieldEncryptedData.setType(SettingVariableTypes.CYBERARK);
      secretFieldEncryptedData.setName(cyberArkConfig.getName() + clientCertNameSuffix);
      secretFieldEncryptedDataId = wingsPersistence.save(secretFieldEncryptedData);
    }
    return secretFieldEncryptedDataId;
  }

  @Override
  public boolean deleteConfig(String accountId, String configId) {
    final long count = wingsPersistence.createQuery(EncryptedData.class)
                           .filter(ACCOUNT_ID_KEY, accountId)
                           .filter(EncryptedDataKeys.kmsId, configId)
                           .filter(EncryptedDataKeys.encryptionType, EncryptionType.CYBERARK)
                           .count(new CountOptions().limit(1));

    if (count > 0) {
      String message = "Can not delete the CyberArk configuration since there are secrets encrypted with this. "
          + "Please transition your secrets to another secret manager and try again.";
      throw new WingsException(CYBERARK_OPERATION_ERROR, USER).addParam(REASON_KEY, message);
    }

    CyberArkConfig cyberArkConfig = wingsPersistence.get(CyberArkConfig.class, configId);
    Preconditions.checkNotNull(cyberArkConfig, "no Aws Secrets Manager config found with id " + configId);

    if (isNotEmpty(cyberArkConfig.getClientCertificate())) {
      wingsPersistence.delete(EncryptedData.class, cyberArkConfig.getClientCertificate());
      logger.info("Deleted encrypted auth token record {} associated with CyberArk Secrets Manager '{}'",
          cyberArkConfig.getClientCertificate(), cyberArkConfig.getName());
    }

    return wingsPersistence.delete(SecretManagerConfig.class, configId);
  }

  @Override
  public void validateConfig(CyberArkConfig cyberArkConfig) {
    String errorMessage;
    if (!Http.connectableHost(cyberArkConfig.getCyberArkUrl())) {
      errorMessage = "Was not able to reach CyberArk using given URL. Please check your configurations and try again";
      throw new WingsException(CYBERARK_OPERATION_ERROR, errorMessage, USER).addParam(REASON_KEY, errorMessage);
    } else if (isNotEmpty(cyberArkConfig.getClientCertificate())
        && !CyberArkRestClientFactory.validateClientCertificate(cyberArkConfig.getClientCertificate())) {
      errorMessage = "Client certificate provided is not valid. Please check your configurations and try again";
      throw new WingsException(CYBERARK_OPERATION_ERROR, errorMessage, USER).addParam(REASON_KEY, errorMessage);
    }
  }

  @Override
  public void decryptCyberArkConfigSecrets(String accountId, CyberArkConfig cyberArkConfig, boolean maskSecret) {
    EncryptedData encryptedClientCert =
        wingsPersistence.get(EncryptedData.class, cyberArkConfig.getClientCertificate());
    if (encryptedClientCert != null) {
      if (maskSecret) {
        cyberArkConfig.maskSecrets();
      } else {
        cyberArkConfig.setClientCertificate(String.valueOf(decryptLocal(encryptedClientCert)));
      }
    }
  }
}
