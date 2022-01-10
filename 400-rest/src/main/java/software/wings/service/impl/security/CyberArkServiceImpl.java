/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.security;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.CYBERARK_OPERATION_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.persistence.HPersistence.upToOne;

import static software.wings.beans.CGConstants.GLOBAL_APP_ID;
import static software.wings.settings.SettingVariableTypes.CYBERARK;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EncryptedData;
import io.harness.beans.EncryptedData.EncryptedDataKeys;
import io.harness.beans.EncryptedDataParent;
import io.harness.beans.SecretManagerConfig.SecretManagerConfigKeys;
import io.harness.exception.SecretManagementException;
import io.harness.security.encryption.EncryptionType;
import io.harness.serializer.KryoSerializer;

import software.wings.beans.CyberArkConfig;
import software.wings.beans.CyberArkConfig.CyberArkConfigKeys;
import software.wings.beans.SyncTaskContext;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.security.CyberArkService;
import software.wings.service.intfc.security.SecretManagementDelegateService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.DuplicateKeyException;
import java.time.Duration;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;

/**
 * @author marklu on 2019-08-01
 */
@OwnedBy(PL)
@Singleton
@Slf4j
public class CyberArkServiceImpl extends AbstractSecretServiceImpl implements CyberArkService {
  private static final String CLIENT_CERTIFICATE_NAME_SUFFIX = "_clientCertificate";

  @Inject private KryoSerializer kryoSerializer;
  @Inject private AccountService accountService;

  @Override
  public CyberArkConfig getConfig(String accountId, String configId) {
    CyberArkConfig cyberArkConfig = wingsPersistence.createQuery(CyberArkConfig.class)
                                        .filter(SecretManagerConfigKeys.accountId, accountId)
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
    if (cyberArkConfig.isDefault()) {
      throw new SecretManagementException(
          CYBERARK_OPERATION_ERROR, "Cyberark Secrets Manager cannot be set to default", USER);
    }
    CyberArkConfig oldConfigForAudit = null;
    CyberArkConfig savedConfig = null;
    boolean credentialChanged = true;
    if (!isEmpty(cyberArkConfig.getUuid())) {
      savedConfig = getConfig(accountId, cyberArkConfig.getUuid());
      if (SECRET_MASK.equals(cyberArkConfig.getClientCertificate())) {
        cyberArkConfig.setClientCertificate(savedConfig.getClientCertificate());
      }
      credentialChanged =
          (!SECRET_MASK.equals(cyberArkConfig.getClientCertificate())
              && !Objects.equals(cyberArkConfig.getClientCertificate(), savedConfig.getClientCertificate()))
          || !Objects.equals(cyberArkConfig.getCyberArkUrl(), savedConfig.getCyberArkUrl())
          || !Objects.equals(cyberArkConfig.getAppId(), savedConfig.getAppId());

      // Secret field un-decrypted version of saved config
      savedConfig = wingsPersistence.get(CyberArkConfig.class, cyberArkConfig.getUuid());
      oldConfigForAudit = kryoSerializer.clone(savedConfig);
    }

    // Validate every time when secret manager config change submitted
    validateConfig(cyberArkConfig);

    if (!credentialChanged) {
      // update without client certificate or url changes
      savedConfig.setName(cyberArkConfig.getName());
      savedConfig.setDefault(cyberArkConfig.isDefault());
      savedConfig.setUsageRestrictions(cyberArkConfig.getUsageRestrictions());
      savedConfig.setScopedToAccount(cyberArkConfig.isScopedToAccount());

      // PL-3237: Audit secret manager config changes.
      generateAuditForSecretManager(accountId, oldConfigForAudit, savedConfig);

      return secretManagerConfigService.save(savedConfig);
    }

    EncryptedData clientCertEncryptedData =
        getEncryptedDataForClientCertificateField(savedConfig, cyberArkConfig, cyberArkConfig.getClientCertificate());

    cyberArkConfig.setClientCertificate(null);
    String secretsManagerConfigId;
    try {
      secretsManagerConfigId = secretManagerConfigService.save(cyberArkConfig);
    } catch (DuplicateKeyException e) {
      String message = "Another CyberArk secret manager with the same name or URL exists";
      throw new SecretManagementException(CYBERARK_OPERATION_ERROR, message, USER_SRE);
    }

    // Create a LOCAL encrypted record for AWS secret key
    String clientCertEncryptedDataId = saveClientCertificateField(cyberArkConfig, secretsManagerConfigId,
        clientCertEncryptedData, CLIENT_CERTIFICATE_NAME_SUFFIX, CyberArkConfigKeys.clientCertificate);
    cyberArkConfig.setClientCertificate(clientCertEncryptedDataId);

    // PL-3237: Audit secret manager config changes.
    generateAuditForSecretManager(accountId, oldConfigForAudit, cyberArkConfig);

    return secretManagerConfigService.save(cyberArkConfig);
  }

  private EncryptedData getEncryptedDataForClientCertificateField(
      CyberArkConfig savedConfig, CyberArkConfig cyberArkConfig, String clientCertificate) {
    EncryptedData encryptedData = isNotEmpty(clientCertificate) && !Objects.equals(SECRET_MASK, clientCertificate)
        ? encryptUsingBaseAlgo(cyberArkConfig.getAccountId(), clientCertificate.toCharArray())
        : null;

    EncryptedData savedEncryptedData = null;
    if (savedConfig != null) {
      // Get by auth token encrypted record by Id or name.
      Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class);
      query.criteria(EncryptedDataKeys.accountId)
          .equal(cyberArkConfig.getAccountId())
          .or(query.criteria(ID_KEY).equal(savedConfig.getClientCertificate()),
              query.criteria(EncryptedDataKeys.name).equal(cyberArkConfig.getName() + CLIENT_CERTIFICATE_NAME_SUFFIX));
      savedEncryptedData = query.get();
    }
    if (savedEncryptedData != null && encryptedData != null) {
      savedEncryptedData.setEncryptionKey(encryptedData.getEncryptionKey());
      savedEncryptedData.setEncryptedValue(encryptedData.getEncryptedValue());
      savedEncryptedData.setEncryptionType(encryptedData.getEncryptionType());
      savedEncryptedData.setKmsId(encryptedData.getKmsId());
      return savedEncryptedData;
    } else {
      return encryptedData;
    }
  }

  private String saveClientCertificateField(CyberArkConfig cyberArkConfig, String configId,
      EncryptedData secretFieldEncryptedData, String clientCertNameSuffix, String fieldName) {
    String secretFieldEncryptedDataId = null;
    if (secretFieldEncryptedData != null) {
      secretFieldEncryptedData.setAccountId(cyberArkConfig.getAccountId());
      secretFieldEncryptedData.addParent(
          EncryptedDataParent.createParentRef(configId, CyberArkConfig.class, fieldName, CYBERARK));
      secretFieldEncryptedData.setType(CYBERARK);
      secretFieldEncryptedData.setName(cyberArkConfig.getName() + clientCertNameSuffix);
      secretFieldEncryptedDataId = wingsPersistence.save(secretFieldEncryptedData);
    }
    return secretFieldEncryptedDataId;
  }

  @Override
  public boolean deleteConfig(String accountId, String configId) {
    long count = wingsPersistence.createQuery(EncryptedData.class)
                     .filter(EncryptedDataKeys.accountId, accountId)
                     .filter(EncryptedDataKeys.kmsId, configId)
                     .filter(EncryptedDataKeys.encryptionType, EncryptionType.CYBERARK)
                     .count(upToOne);

    if (count > 0) {
      String message = "Cannot delete the CyberArk configuration since there are secrets encrypted with it. "
          + "Please transition your secrets to another secret manager and try again.";
      throw new SecretManagementException(CYBERARK_OPERATION_ERROR, message, USER);
    }

    CyberArkConfig cyberArkConfig = wingsPersistence.get(CyberArkConfig.class, configId);
    checkNotNull(cyberArkConfig, "No CyberArk secret manager configuration found with id " + configId);

    if (isNotEmpty(cyberArkConfig.getClientCertificate())) {
      wingsPersistence.delete(EncryptedData.class, cyberArkConfig.getClientCertificate());
      log.info("Deleted encrypted auth token record {} associated with CyberArk Secrets Manager '{}'",
          cyberArkConfig.getClientCertificate(), cyberArkConfig.getName());
    }

    return deleteSecretManagerAndGenerateAudit(accountId, cyberArkConfig);
  }

  @Override
  public void validateConfig(CyberArkConfig cyberArkConfig) {
    if (isEmpty(cyberArkConfig.getName())) {
      throw new SecretManagementException(CYBERARK_OPERATION_ERROR, "Name can not be empty", USER);
    }

    SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                          .accountId(cyberArkConfig.getAccountId())
                                          .timeout(Duration.ofSeconds(10).toMillis())
                                          .appId(GLOBAL_APP_ID)
                                          .correlationId(cyberArkConfig.getUuid())
                                          .build();
    boolean isCertValidationRequired = accountService.isCertValidationRequired(cyberArkConfig.getAccountId());
    cyberArkConfig.setCertValidationRequired(isCertValidationRequired);
    delegateProxyFactory.get(SecretManagementDelegateService.class, syncTaskContext)
        .validateCyberArkConfig(cyberArkConfig);
  }

  @Override
  public void decryptCyberArkConfigSecrets(String accountId, CyberArkConfig cyberArkConfig, boolean maskSecret) {
    EncryptedData encryptedClientCert =
        wingsPersistence.get(EncryptedData.class, cyberArkConfig.getClientCertificate());
    if (encryptedClientCert != null) {
      if (maskSecret) {
        cyberArkConfig.maskSecrets();
      } else {
        cyberArkConfig.setClientCertificate(String.valueOf(decryptUsingAlgoOfSecret(encryptedClientCert)));
      }
    }
  }
}
