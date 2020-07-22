package software.wings.service.intfc.security;

import static io.harness.validation.Validator.notEmptyCheck;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.secretmanagerclient.NGMetadata;
import io.harness.secretmanagerclient.NGMetadata.NGMetadataKeys;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptionType;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.query.Query;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.SecretManagerConfig;
import software.wings.beans.SecretManagerConfig.SecretManagerConfigKeys;
import software.wings.beans.VaultConfig;
import software.wings.dl.WingsPersistence;

import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;

public class NGSecretManagerServiceImpl implements NGSecretManagerService {
  @Inject private SecretManager secretManager;
  @Inject private VaultService vaultService;
  @Inject private WingsPersistence wingsPersistence;

  private static final String ACCOUNT_IDENTIFIER_KEY =
      SecretManagerConfigKeys.ngMetadata + "." + NGMetadataKeys.accountIdentifier;

  private static final String ORG_IDENTIFIER_KEY =
      SecretManagerConfigKeys.ngMetadata + "." + NGMetadataKeys.orgIdentifier;

  private static final String PROJECT_IDENTIFIER_KEY =
      SecretManagerConfigKeys.ngMetadata + "." + NGMetadataKeys.projectIdentifier;

  private static final String IDENTIFIER_KEY = SecretManagerConfigKeys.ngMetadata + "." + NGMetadataKeys.identifier;

  @Override
  public String createSecretManager(SecretManagerConfig secretManagerConfig) {
    NGMetadata ngMetadata = secretManagerConfig.getNgMetadata();
    if (Optional.ofNullable(ngMetadata).isPresent()) {
      if (secretManagerConfig.getEncryptionType() == EncryptionType.VAULT) {
        vaultService.saveOrUpdateVaultConfig(secretManagerConfig.getAccountId(), (VaultConfig) secretManagerConfig);
      }
    }
    throw new UnsupportedOperationException("secret manager not supported in NG");
  }

  private Query<SecretManagerConfig> getQuery(
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return wingsPersistence.createQuery(SecretManagerConfig.class)
        .field(ACCOUNT_IDENTIFIER_KEY)
        .equal(accountIdentifier)
        .field(ORG_IDENTIFIER_KEY)
        .equal(orgIdentifier)
        .field(PROJECT_IDENTIFIER_KEY)
        .equal(projectIdentifier);
  }

  @Override
  public List<SecretManagerConfig> listSecretManagers(
      @NotNull String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    Query<SecretManagerConfig> secretManagerConfigQuery;
    if (!StringUtils.isEmpty(projectIdentifier)) {
      notEmptyCheck("Account/Org identifier cannot be empty", Lists.newArrayList(accountIdentifier, orgIdentifier));
      secretManagerConfigQuery = getQuery(accountIdentifier, orgIdentifier, projectIdentifier);
    } else if (!StringUtils.isEmpty(orgIdentifier)) {
      notEmptyCheck("Account identifier be empty", Lists.newArrayList(accountIdentifier));
      secretManagerConfigQuery = getQuery(accountIdentifier, orgIdentifier, null);
    } else {
      secretManagerConfigQuery = getQuery(accountIdentifier, null, null);
    }
    return secretManagerConfigQuery.asList();
  }

  @Override
  public SecretManagerConfig getSecretManager(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    Query<SecretManagerConfig> secretManagerConfigQuery = getQuery(accountIdentifier, orgIdentifier, projectIdentifier);
    secretManagerConfigQuery.field(IDENTIFIER_KEY).equal(identifier);
    return secretManagerConfigQuery.get();
  }

  @Override
  public String updateSecretManager(SecretManagerConfig secretManagerConfig) {
    if (secretManagerConfig.getEncryptionType() == EncryptionType.VAULT) {
      return vaultService.saveOrUpdateVaultConfig(
          secretManagerConfig.getAccountId(), (VaultConfig) secretManagerConfig);
    }
    throw new UnsupportedOperationException("Secret Manager not supported in NG");
  }

  @Override
  public List<EncryptedDataDetail> getEncryptionDetails(EncryptableSetting object) {
    return secretManager.getEncryptionDetails(object);
  }

  @Override
  public boolean deleteSecretManager(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    SecretManagerConfig secretManagerConfig =
        getSecretManager(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    if (Optional.ofNullable(secretManagerConfig).isPresent()) {
      switch (secretManagerConfig.getEncryptionType()) {
        case VAULT:
          VaultConfig vaultConfig = (VaultConfig) secretManagerConfig;
          return vaultService.deleteVaultConfig(vaultConfig.getAccountId(), vaultConfig.getUuid());
        case GCP_KMS:
          // TODO{phoenikx} Support GCP KMS soon
        default:
          throw new UnsupportedOperationException("Secret manager not supported");
      }
    }
    return false;
  }
}
