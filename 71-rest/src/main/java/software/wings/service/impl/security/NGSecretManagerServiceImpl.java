package software.wings.service.impl.security;

import static io.harness.security.encryption.EncryptionType.GCP_KMS;
import static io.harness.security.encryption.EncryptionType.LOCAL;

import com.google.inject.Inject;

import io.harness.exception.DuplicateFieldException;
import io.harness.secretmanagerclient.NGMetadata.NGMetadataKeys;
import io.harness.secretmanagerclient.NGSecretManagerMetadata;
import io.harness.secretmanagerclient.NGSecretManagerMetadata.NGSecretManagerMetadataKeys;
import io.harness.security.encryption.EncryptedDataDetail;
import org.mongodb.morphia.query.Query;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.GcpKmsConfig;
import software.wings.beans.LocalEncryptionConfig;
import software.wings.beans.SecretManagerConfig;
import software.wings.beans.SecretManagerConfig.SecretManagerConfigKeys;
import software.wings.beans.VaultConfig;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.security.GcpSecretsManagerService;
import software.wings.service.intfc.security.LocalEncryptionService;
import software.wings.service.intfc.security.NGSecretManagerService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.security.SecretManagerConfigService;
import software.wings.service.intfc.security.VaultService;

import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;

public class NGSecretManagerServiceImpl implements NGSecretManagerService {
  @Inject private SecretManager secretManager;
  @Inject private VaultService vaultService;
  @Inject private LocalEncryptionService localEncryptionService;
  @Inject private GcpSecretsManagerService gcpSecretsManagerService;
  @Inject private SecretManagerConfigService secretManagerConfigService;
  @Inject private WingsPersistence wingsPersistence;

  private static final String ACCOUNT_IDENTIFIER_KEY =
      SecretManagerConfigKeys.ngMetadata + "." + NGSecretManagerMetadataKeys.accountIdentifier;

  private static final String ORG_IDENTIFIER_KEY =
      SecretManagerConfigKeys.ngMetadata + "." + NGSecretManagerMetadataKeys.orgIdentifier;

  private static final String PROJECT_IDENTIFIER_KEY =
      SecretManagerConfigKeys.ngMetadata + "." + NGSecretManagerMetadataKeys.projectIdentifier;

  private static final String IDENTIFIER_KEY = SecretManagerConfigKeys.ngMetadata + "." + NGMetadataKeys.identifier;

  @Override
  public SecretManagerConfig createSecretManager(SecretManagerConfig secretManagerConfig) {
    NGSecretManagerMetadata ngMetadata = secretManagerConfig.getNgMetadata();
    if (Optional.ofNullable(ngMetadata).isPresent()) {
      // TODO{phoenikx} Do it using a DB index
      boolean duplicatePresent = checkForDuplicate(ngMetadata.getAccountIdentifier(), ngMetadata.getOrgIdentifier(),
          ngMetadata.getProjectIdentifier(), ngMetadata.getIdentifier());
      if (duplicatePresent) {
        throw new DuplicateFieldException("Secret manager with same configuration exists");
      }
      switch (secretManagerConfig.getEncryptionType()) {
        case VAULT:
          vaultService.saveOrUpdateVaultConfig(
              secretManagerConfig.getAccountId(), (VaultConfig) secretManagerConfig, false);
          return secretManagerConfig;
        case GCP_KMS:
          gcpSecretsManagerService.saveGcpKmsConfig(
              secretManagerConfig.getAccountId(), (GcpKmsConfig) secretManagerConfig, false);
          return secretManagerConfig;
        case LOCAL:
          localEncryptionService.saveLocalEncryptionConfig(
              secretManagerConfig.getAccountId(), (LocalEncryptionConfig) secretManagerConfig);
          return secretManagerConfig;
        default:
          throw new UnsupportedOperationException("secret manager not supported in NG");
      }
    }
    throw new UnsupportedOperationException("secret manager not supported in NG");
  }

  private boolean checkForDuplicate(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    return getSecretManager(accountIdentifier, orgIdentifier, projectIdentifier, identifier).isPresent();
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
    secretManagerConfigQuery = getQuery(accountIdentifier, orgIdentifier, projectIdentifier);
    return secretManagerConfigQuery.asList();
  }

  @Override
  public Optional<SecretManagerConfig> getSecretManager(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    Query<SecretManagerConfig> secretManagerConfigQuery = getQuery(accountIdentifier, orgIdentifier, projectIdentifier);
    secretManagerConfigQuery.field(IDENTIFIER_KEY).equal(identifier);
    return Optional.ofNullable(secretManagerConfigQuery.get());
  }

  @Override
  public SecretManagerConfig getGlobalSecretManager(String accountIdentifier) {
    SecretManagerConfig accountSecretManagerConfig =
        secretManagerConfigService.getGlobalSecretManager(accountIdentifier);
    if (accountSecretManagerConfig == null || accountSecretManagerConfig.getEncryptionType() != GCP_KMS) {
      accountSecretManagerConfig = localEncryptionService.getEncryptionConfig(accountIdentifier);
      accountSecretManagerConfig.setUuid(null);
      accountSecretManagerConfig.setEncryptionType(LOCAL);
    }
    return accountSecretManagerConfig;
  }

  @Override
  public SecretManagerConfig updateSecretManager(SecretManagerConfig secretManagerConfig) {
    switch (secretManagerConfig.getEncryptionType()) {
      case VAULT:
        vaultService.saveOrUpdateVaultConfig(
            secretManagerConfig.getAccountId(), (VaultConfig) secretManagerConfig, false);
        return secretManagerConfig;
      case GCP_KMS:
        gcpSecretsManagerService.updateGcpKmsConfig(
            secretManagerConfig.getAccountId(), (GcpKmsConfig) secretManagerConfig, false);
        return secretManagerConfig;
      default:
        throw new UnsupportedOperationException("Secret Manager not supported in NG");
    }
  }

  @Override
  public List<EncryptedDataDetail> getEncryptionDetails(EncryptableSetting object) {
    return secretManager.getEncryptionDetails(object);
  }

  @Override
  public boolean deleteSecretManager(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    Optional<SecretManagerConfig> secretManagerConfigOptional =
        getSecretManager(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    if (secretManagerConfigOptional.isPresent()) {
      SecretManagerConfig secretManagerConfig = secretManagerConfigOptional.get();
      switch (secretManagerConfig.getEncryptionType()) {
        case VAULT:
          VaultConfig vaultConfig = (VaultConfig) secretManagerConfig;
          return vaultService.deleteVaultConfig(vaultConfig.getAccountId(), vaultConfig.getUuid());
        case GCP_KMS:
          GcpKmsConfig gcpKmsConfig = (GcpKmsConfig) secretManagerConfig;
          return gcpSecretsManagerService.deleteGcpKmsConfig(gcpKmsConfig.getAccountId(), gcpKmsConfig.getUuid());
        default:
          throw new UnsupportedOperationException("Secret manager not supported");
      }
    }
    return false;
  }
}
