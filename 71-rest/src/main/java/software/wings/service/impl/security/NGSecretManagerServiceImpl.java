package software.wings.service.impl.security;

import static io.harness.security.encryption.EncryptionType.GCP_KMS;
import static io.harness.security.encryption.EncryptionType.LOCAL;
import static io.harness.security.encryption.EncryptionType.VAULT;

import io.harness.beans.SecretManagerConfig;
import io.harness.beans.SecretManagerConfig.SecretManagerConfigKeys;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.SecretManagementException;
import io.harness.helpers.ext.vault.SecretEngineSummary;
import io.harness.helpers.ext.vault.VaultAppRoleLoginResult;
import io.harness.secretmanagerclient.NGMetadata.NGMetadataKeys;
import io.harness.secretmanagerclient.NGSecretManagerMetadata;
import io.harness.secretmanagerclient.NGSecretManagerMetadata.NGSecretManagerMetadataKeys;
import io.harness.secretmanagerclient.dto.SecretManagerMetadataDTO;
import io.harness.secretmanagerclient.dto.SecretManagerMetadataRequestDTO;
import io.harness.secretmanagerclient.dto.VaultAppRoleCredentialDTO;
import io.harness.secretmanagerclient.dto.VaultAuthTokenCredentialDTO;
import io.harness.secretmanagerclient.dto.VaultMetadataRequestSpecDTO;
import io.harness.secretmanagerclient.dto.VaultMetadataSpecDTO;
import io.harness.secretmanagerclient.dto.VaultSecretEngineDTO;
import io.harness.secretmanagers.SecretManagerConfigService;
import io.harness.security.encryption.AccessType;
import io.harness.security.encryption.EncryptionType;

import software.wings.beans.GcpKmsConfig;
import software.wings.beans.LocalEncryptionConfig;
import software.wings.beans.VaultConfig;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.security.GcpSecretsManagerService;
import software.wings.service.intfc.security.LocalSecretManagerService;
import software.wings.service.intfc.security.NGSecretManagerService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.security.VaultService;

import com.google.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;

@Slf4j
public class NGSecretManagerServiceImpl implements NGSecretManagerService {
  @Inject private SecretManager secretManager;
  @Inject private VaultService vaultService;
  @Inject private LocalSecretManagerService localSecretManagerService;
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
          VaultConfig vaultConfig = (VaultConfig) secretManagerConfig;
          if (vaultConfig.getAccessType() == AccessType.APP_ROLE) {
            VaultAppRoleLoginResult loginResult = vaultService.appRoleLogin(vaultConfig);
            if (loginResult != null && EmptyPredicate.isNotEmpty(loginResult.getClientToken())) {
              vaultConfig.setAuthToken(loginResult.getClientToken());
            }
          }
          vaultService.saveOrUpdateVaultConfig(
              secretManagerConfig.getAccountId(), (VaultConfig) secretManagerConfig, false);
          return secretManagerConfig;
        case GCP_KMS:
          gcpSecretsManagerService.saveGcpKmsConfig(
              secretManagerConfig.getAccountId(), (GcpKmsConfig) secretManagerConfig, false);
          return secretManagerConfig;
        case LOCAL:
          localSecretManagerService.saveLocalEncryptionConfig(
              secretManagerConfig.getAccountId(), (LocalEncryptionConfig) secretManagerConfig);
          return secretManagerConfig;
        default:
          throw new UnsupportedOperationException("secret manager not supported in NG");
      }
    }
    throw new UnsupportedOperationException("secret manager not supported in NG");
  }

  @Override
  public boolean validate(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    Optional<SecretManagerConfig> secretManagerConfigOptional =
        getSecretManager(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    if (secretManagerConfigOptional.isPresent()) {
      secretManagerConfigService.decryptEncryptionConfigSecrets(
          accountIdentifier, secretManagerConfigOptional.get(), false);
      try {
        switch (secretManagerConfigOptional.get().getEncryptionType()) {
          case VAULT:
            vaultService.validateVaultConfig(accountIdentifier, (VaultConfig) secretManagerConfigOptional.get());
            return true;
          case GCP_KMS:
            gcpSecretsManagerService.validateSecretsManagerConfig(
                accountIdentifier, (GcpKmsConfig) secretManagerConfigOptional.get());
            return true;
          case LOCAL:
            localSecretManagerService.validateLocalEncryptionConfig(
                accountIdentifier, (LocalEncryptionConfig) secretManagerConfigOptional.get());
            return true;
          default:
            return false;
        }
      } catch (SecretManagementException secretManagementException) {
        log.info("Error while validating secret manager config with details: {}, {}, {}, {}, error: ",
            accountIdentifier, orgIdentifier, projectIdentifier, identifier, secretManagementException);
      }
    }
    return false;
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
      accountSecretManagerConfig = localSecretManagerService.getEncryptionConfig(accountIdentifier);
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

  private VaultSecretEngineDTO fromSecretEngineSummary(SecretEngineSummary secretEngineSummary) {
    if (secretEngineSummary == null) {
      return null;
    }
    return VaultSecretEngineDTO.builder()
        .name(secretEngineSummary.getName())
        .description(secretEngineSummary.getDescription())
        .type(secretEngineSummary.getType())
        .version(secretEngineSummary.getVersion())
        .build();
  }

  @Override
  public SecretManagerMetadataDTO getMetadata(String accountIdentifier, SecretManagerMetadataRequestDTO requestDTO) {
    if (requestDTO.getEncryptionType() == EncryptionType.VAULT) {
      VaultConfig vaultConfig;
      Optional<SecretManagerConfig> secretManagerConfigOptional = getSecretManager(accountIdentifier,
          requestDTO.getOrgIdentifier(), requestDTO.getProjectIdentifier(), requestDTO.getIdentifier());
      if (secretManagerConfigOptional.isPresent()) {
        vaultConfig = (VaultConfig) secretManagerConfigOptional.get();
        secretManagerConfigService.decryptEncryptionConfigSecrets(vaultConfig.getAccountId(), vaultConfig, false);
      } else {
        VaultMetadataRequestSpecDTO vaultMetadataRequestSpecDTO = (VaultMetadataRequestSpecDTO) requestDTO.getSpec();
        vaultConfig = VaultConfig.builder().vaultUrl(vaultMetadataRequestSpecDTO.getUrl()).build();
        vaultConfig.setAccountId(accountIdentifier);
        if (vaultMetadataRequestSpecDTO.getAccessType() == AccessType.APP_ROLE) {
          vaultConfig.setAppRoleId(((VaultAppRoleCredentialDTO) vaultMetadataRequestSpecDTO.getSpec()).getAppRoleId());
          vaultConfig.setSecretId(((VaultAppRoleCredentialDTO) vaultMetadataRequestSpecDTO.getSpec()).getSecretId());
        } else {
          vaultConfig.setAuthToken(
              ((VaultAuthTokenCredentialDTO) vaultMetadataRequestSpecDTO.getSpec()).getAuthToken());
        }
      }
      List<SecretEngineSummary> secretEngineSummaryList = vaultService.listSecretEngines(vaultConfig);
      return SecretManagerMetadataDTO.builder()
          .encryptionType(VAULT)
          .spec(
              VaultMetadataSpecDTO.builder()
                  .secretEngines(
                      secretEngineSummaryList.stream().map(this::fromSecretEngineSummary).collect(Collectors.toList()))
                  .build())
          .build();
    }
    throw new UnsupportedOperationException(
        "This API is not supported for secret manager of type: " + requestDTO.getEncryptionType());
  }
}
