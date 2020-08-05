package software.wings.service.intfc.security;

import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.eraro.ErrorCode.ENCRYPT_DECRYPT_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.security.encryption.EncryptionType.VAULT;

import com.google.inject.Inject;

import io.harness.beans.DecryptableEntity;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.NGAccess;
import io.harness.secretmanagerclient.NGEncryptedDataMetadata;
import io.harness.secretmanagerclient.NGMetadata.NGMetadataKeys;
import io.harness.secretmanagerclient.NGSecretManagerMetadata.NGSecretManagerMetadataKeys;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptedRecordData;
import org.mongodb.morphia.query.Query;
import software.wings.beans.SecretManagerConfig;
import software.wings.beans.VaultConfig;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedData.EncryptedDataKeys;
import software.wings.service.impl.security.SecretManagementException;
import software.wings.settings.SettingVariableTypes;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;

public class NGSecretServiceImpl implements NGSecretService {
  private static final String ACCOUNT_IDENTIFIER_KEY =
      EncryptedDataKeys.ngMetadata + "." + NGSecretManagerMetadataKeys.accountIdentifier;
  private static final String ORG_IDENTIFIER_KEY =
      EncryptedDataKeys.ngMetadata + "." + NGSecretManagerMetadataKeys.orgIdentifier;
  private static final String IDENTIFIER_KEY = EncryptedDataKeys.ngMetadata + "." + NGMetadataKeys.identifier;
  private static final String TAGS_KEY = EncryptedDataKeys.ngMetadata + "." + NGSecretManagerMetadataKeys.tags;
  private static final String PROJECT_IDENTIFIER_KEY =
      EncryptedDataKeys.ngMetadata + "." + NGSecretManagerMetadataKeys.projectIdentifier;

  @Inject private NGSecretManagerService ngSecretManagerService;
  @Inject private SecretManager secretManager;
  @Inject private VaultService vaultService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private SecretManagerConfigService secretManagerConfigService;

  private EncryptedData encrypt(@NotNull EncryptedData encryptedData, SettingVariableTypes type, String secretValue,
      SecretManagerConfig secretManagerConfig) {
    if (encryptedData.getEncryptionType() == VAULT) {
      vaultService.decryptVaultConfigSecrets(
          secretManagerConfig.getAccountId(), (VaultConfig) secretManagerConfig, false);
      return vaultService.encrypt(encryptedData.getName(), secretValue, encryptedData.getAccountId(), type,
          (VaultConfig) secretManagerConfig, encryptedData);
    }
    throw new UnsupportedOperationException("Encryption type not supported: " + encryptedData.getEncryptionType());
  }

  @Override
  public EncryptedData createSecretText(@NotNull EncryptedData data, String secretValue) {
    NGEncryptedDataMetadata metadata = data.getNgMetadata();
    if (Optional.ofNullable(metadata).isPresent()) {
      boolean duplicate = get(metadata.getAccountIdentifier(), metadata.getOrgIdentifier(),
          metadata.getProjectIdentifier(), metadata.getIdentifier())
                              .isPresent();
      if (duplicate) {
        // TODO{phoenikx} remove this and add unique index after migrating existing secrets
        throw new SecretManagementException(
            ErrorCode.SECRET_MANAGEMENT_ERROR, "Duplicate secret text present", USER_SRE);
      }
      Optional<SecretManagerConfig> secretManagerConfigOptional =
          ngSecretManagerService.getSecretManager(metadata.getAccountIdentifier(), metadata.getOrgIdentifier(),
              metadata.getProjectIdentifier(), metadata.getSecretManagerIdentifier());
      if (secretManagerConfigOptional.isPresent()) {
        SecretManagerConfig secretManagerConfig = secretManagerConfigOptional.get();
        secretManager.validateSecretPath(secretManagerConfig.getEncryptionType(), data.getPath());
        data.setEncryptionType(secretManagerConfig.getEncryptionType());
        data.setKmsId(secretManagerConfig.getUuid());
        EncryptedData encryptedData = encrypt(data, SettingVariableTypes.SECRET_TEXT, secretValue, secretManagerConfig);
        if (Optional.ofNullable(encryptedData).isPresent()) {
          secretManager.saveEncryptedData(encryptedData);
        }
        return encryptedData;
      }
    }
    return null;
  }

  @Override
  public PageResponse<EncryptedData> listSecrets(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, SettingVariableTypes type, String limit, String offset) {
    PageRequest<EncryptedData> pageRequest = new PageRequest<>();
    pageRequest.addFilter(ACCOUNT_IDENTIFIER_KEY, EQ, accountIdentifier);
    pageRequest.addFilter(ORG_IDENTIFIER_KEY, EQ, orgIdentifier);
    pageRequest.addFilter(PROJECT_IDENTIFIER_KEY, EQ, projectIdentifier);
    if (Optional.ofNullable(type).isPresent()) {
      pageRequest.addFilter(EncryptedDataKeys.type, EQ, type);
    }
    pageRequest.setLimit(limit);
    pageRequest.setOffset(offset);
    return wingsPersistence.query(EncryptedData.class, pageRequest);
  }

  @Override
  public Optional<EncryptedData> get(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    return Optional.ofNullable(wingsPersistence.createQuery(EncryptedData.class)
                                   .field(ACCOUNT_IDENTIFIER_KEY)
                                   .equal(accountIdentifier)
                                   .field(ORG_IDENTIFIER_KEY)
                                   .equal(orgIdentifier)
                                   .field(PROJECT_IDENTIFIER_KEY)
                                   .equal(projectIdentifier)
                                   .field(IDENTIFIER_KEY)
                                   .equal(identifier)
                                   .get());
  }

  @Override
  public boolean updateSecretText(@NotNull EncryptedData encryptedData, String secretValue) {
    secretManager.validateSecretPath(encryptedData.getEncryptionType(), encryptedData.getPath());
    NGEncryptedDataMetadata metadata = encryptedData.getNgMetadata();
    if (Optional.ofNullable(metadata).isPresent()) {
      Optional<SecretManagerConfig> secretManagerConfigOptional =
          ngSecretManagerService.getSecretManager(metadata.getAccountIdentifier(), metadata.getOrgIdentifier(),
              metadata.getProjectIdentifier(), metadata.getSecretManagerIdentifier());
      if (secretManagerConfigOptional.isPresent()) {
        EncryptedData updatedEncryptedData =
            encrypt(encryptedData, SettingVariableTypes.SECRET_TEXT, secretValue, secretManagerConfigOptional.get());
        encryptedData.setEncryptionKey(updatedEncryptedData.getEncryptionKey());
        encryptedData.setEncryptedValue(updatedEncryptedData.getEncryptedValue());
        secretManager.saveEncryptedData(encryptedData);
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean deleteSecretText(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    Optional<EncryptedData> encryptedDataOptional =
        get(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    return encryptedDataOptional
        .filter(encryptedData -> wingsPersistence.delete(EncryptedData.class, encryptedData.getUuid()))
        .isPresent();
  }

  private String getOrgIdentifier(String parentOrgIdentifier, @NotNull Scope scope) {
    if (scope != Scope.ACCOUNT) {
      return parentOrgIdentifier;
    }
    return null;
  }

  private String getProjectIdentifier(String parentProjectIdentifier, @NotNull Scope scope) {
    if (scope == Scope.PROJECT) {
      return parentProjectIdentifier;
    }
    return null;
  }

  @Override
  public List<EncryptedDataDetail> getEncryptionDetails(NGAccess ngAccess, DecryptableEntity object) {
    if (object.isDecrypted()) {
      return Collections.emptyList();
    }
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    List<Field> encryptedFields = object.getSecretReferenceFields();
    for (Field field : encryptedFields) {
      try {
        field.setAccessible(true);
        SecretRefData secretRefData = (SecretRefData) field.get(object);
        String secretIdentifier = secretRefData.getIdentifier();
        Scope secretScope = secretRefData.getScope();
        if (Optional.ofNullable(secretIdentifier).isPresent() && Optional.ofNullable(secretScope).isPresent()) {
          String accountIdentifier = ngAccess.getAccountIdentifier();
          String orgIdentifier = getOrgIdentifier(ngAccess.getOrgIdentifier(), secretScope);
          String projectIdentifier = getProjectIdentifier(ngAccess.getProjectIdentifier(), secretScope);
          Optional<EncryptedData> encryptedDataOptional =
              get(accountIdentifier, orgIdentifier, projectIdentifier, secretIdentifier);
          if (encryptedDataOptional.isPresent()
              && Optional.ofNullable(encryptedDataOptional.get().getNgMetadata()).isPresent()) {
            EncryptedData encryptedData = encryptedDataOptional.get();
            Optional<SecretManagerConfig> secretManagerConfigOptional =
                ngSecretManagerService.getSecretManager(accountIdentifier, orgIdentifier, projectIdentifier,
                    encryptedData.getNgMetadata().getSecretManagerIdentifier());
            if (secretManagerConfigOptional.isPresent()) {
              SecretManagerConfig encryptionConfig = secretManagerConfigOptional.get();
              secretManagerConfigService.decryptEncryptionConfigSecrets(accountIdentifier, encryptionConfig, false);
              EncryptedRecordData encryptedRecordData = SecretManager.buildRecordData(encryptedData);
              encryptedDataDetails.add(EncryptedDataDetail.builder()
                                           .encryptedData(encryptedRecordData)
                                           .encryptionConfig(encryptionConfig)
                                           .fieldName(field.getName())
                                           .build());
            }
          }
        } else {
          throw new InvalidRequestException("Secret identifier or scope not present", USER);
        }
      } catch (IllegalAccessException exception) {
        throw new SecretManagementException(ENCRYPT_DECRYPT_ERROR, exception, USER);
      }
    }
    return encryptedDataDetails;
  }

  private Query<EncryptedData> getSearchQuery(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      SettingVariableTypes type, String searchTerm) {
    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class)
                                     .field(ACCOUNT_IDENTIFIER_KEY)
                                     .equal(accountIdentifier)
                                     .field(ORG_IDENTIFIER_KEY)
                                     .equal(orgIdentifier)
                                     .field(PROJECT_IDENTIFIER_KEY)
                                     .equal(projectIdentifier);
    if (type != null) {
      query = query.field(EncryptedDataKeys.type).equal(type);
    }
    query.or(query.criteria(EncryptedDataKeys.name).containsIgnoreCase(searchTerm),
        query.criteria(IDENTIFIER_KEY).containsIgnoreCase(searchTerm),
        query.criteria(TAGS_KEY).containsIgnoreCase(searchTerm));
    return query;
  }

  @Override
  public List<EncryptedData> searchSecrets(@NotNull String accountIdentifier, String orgIdentifier,
      String projectIdentifier, SettingVariableTypes type, String searchTerm) {
    Query<EncryptedData> query = getSearchQuery(accountIdentifier, orgIdentifier, projectIdentifier, type, searchTerm);
    return query.asList();
  }
}
