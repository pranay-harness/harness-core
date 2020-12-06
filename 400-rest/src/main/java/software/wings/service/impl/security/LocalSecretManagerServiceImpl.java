package software.wings.service.impl.security;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.exception.WingsException.USER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.encryptors.KmsEncryptorsRegistry;
import io.harness.exception.SecretManagementException;
import io.harness.secretmanagers.SecretManagerConfigService;

import software.wings.beans.LocalEncryptionConfig;
import software.wings.security.EnvFilter;
import software.wings.security.GenericEntityFilter;
import software.wings.security.UsageRestrictions;
import software.wings.service.intfc.security.LocalSecretManagerService;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * @author marklu on 2019-05-14
 */
@OwnedBy(PL)
@Singleton
@Slf4j
public class LocalSecretManagerServiceImpl implements LocalSecretManagerService {
  @Inject private SecretManagerConfigService secretManagerConfigService;
  @Inject private KmsEncryptorsRegistry kmsEncryptorsRegistry;

  @Override
  public LocalEncryptionConfig getEncryptionConfig(String accountId) {
    return LocalEncryptionConfig.builder()
        .uuid(accountId)
        .accountId(accountId)
        .scopedToAccount(false)
        .usageRestrictions(
            UsageRestrictions.builder()
                .appEnvRestrictions(Sets.newHashSet(
                    UsageRestrictions.AppEnvRestriction.builder()
                        .appFilter(GenericEntityFilter.builder().filterType(GenericEntityFilter.FilterType.ALL).build())
                        .envFilter(EnvFilter.builder().filterTypes(Sets.newHashSet(EnvFilter.FilterType.PROD)).build())
                        .build(),
                    UsageRestrictions.AppEnvRestriction.builder()
                        .appFilter(GenericEntityFilter.builder().filterType(GenericEntityFilter.FilterType.ALL).build())
                        .envFilter(
                            EnvFilter.builder().filterTypes(Sets.newHashSet(EnvFilter.FilterType.NON_PROD)).build())
                        .build()))
                .build())
        .build();
    // As LOCAL secret manager is HIDDEN right now. The following 'numOfEncryptedValues' field is not needed
    // Therefore commenting out the code below.
    //    Query<EncryptedData> encryptedDataQuery = wingsPersistence.createQuery(EncryptedData.class)
    //                                                  .filter(EncryptedDataKeys.accountId, accountId)
    //                                                  .filter(EncryptedDataKeys.kmsId, accountId)
    //                                                  .filter(EncryptedDataKeys.encryptionType, LOCAL);
    //    encryptionConfig.setNumOfEncryptedValue(encryptedDataQuery.asKeyList().size());
  }

  @Override
  public String saveLocalEncryptionConfig(String accountId, LocalEncryptionConfig localEncryptionConfig) {
    localEncryptionConfig.setAccountId(accountId);
    return secretManagerConfigService.save(localEncryptionConfig);
  }

  @Override
  public void validateLocalEncryptionConfig(String accountId, LocalEncryptionConfig localEncryptionConfig) {
    String randomString = generateUuid();
    try {
      kmsEncryptorsRegistry.getKmsEncryptor(localEncryptionConfig)
          .encryptSecret(accountId, randomString, localEncryptionConfig);
    } catch (Exception e) {
      String message = "Contact Harness Support, not able to encrypt using the local secret manager.";
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, message, e, USER);
    }
  }
}
