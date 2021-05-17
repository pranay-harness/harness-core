package software.wings.service.intfc.security;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.delegate.beans.FileBucket.CONFIGS;
import static io.harness.eraro.ErrorCode.ENCRYPT_DECRYPT_ERROR;
import static io.harness.eraro.ErrorCode.INVALID_REQUEST;
import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.secretmanagerclient.ValueType.Inline;
import static io.harness.security.SimpleEncryption.CHARSET;
import static io.harness.security.encryption.EncryptionType.GCP_KMS;
import static io.harness.security.encryption.EncryptionType.KMS;
import static io.harness.security.encryption.EncryptionType.LOCAL;
import static io.harness.security.encryption.EncryptionType.VAULT;

import static software.wings.service.intfc.security.NGSecretManagerService.isReadOnlySecretManager;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.DecryptableEntity;
import io.harness.beans.EncryptedData;
import io.harness.beans.EncryptedData.EncryptedDataKeys;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SecretManagerConfig;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.encryptors.KmsEncryptorsRegistry;
import io.harness.encryptors.VaultEncryptor;
import io.harness.encryptors.VaultEncryptorsRegistry;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.SecretManagementException;
import io.harness.ng.core.NGAccess;
import io.harness.secretmanagerclient.NGEncryptedDataMetadata;
import io.harness.secretmanagerclient.NGMetadata.NGMetadataKeys;
import io.harness.secretmanagerclient.NGSecretManagerMetadata.NGSecretManagerMetadataKeys;
import io.harness.secretmanagerclient.dto.SecretTextDTO;
import io.harness.secretmanagerclient.dto.SecretTextUpdateDTO;
import io.harness.secretmanagers.SecretManagerConfigService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionType;

import software.wings.dl.WingsPersistence;
import software.wings.resources.secretsmanagement.EncryptedDataMapper;
import software.wings.service.impl.security.GlobalEncryptDecryptClient;
import software.wings.service.intfc.FileService;
import software.wings.settings.SettingVariableTypes;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.query.Query;

@OwnedBy(PL)
@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@TargetModule(HarnessModule._950_NG_CORE)
public class NGSecretServiceImpl implements NGSecretService {
  static final Set<EncryptionType> ENCRYPTION_TYPES_REQUIRING_FILE_DOWNLOAD = EnumSet.of(LOCAL, GCP_KMS, KMS);
  private static final String ACCOUNT_IDENTIFIER_KEY =
      EncryptedDataKeys.ngMetadata + "." + NGSecretManagerMetadataKeys.accountIdentifier;
  private static final String ORG_IDENTIFIER_KEY =
      EncryptedDataKeys.ngMetadata + "." + NGSecretManagerMetadataKeys.orgIdentifier;
  private static final String IDENTIFIER_KEY = EncryptedDataKeys.ngMetadata + "." + NGMetadataKeys.identifier;
  private static final String TAGS_KEY = EncryptedDataKeys.ngMetadata + "." + NGSecretManagerMetadataKeys.tags;
  private static final String PROJECT_IDENTIFIER_KEY =
      EncryptedDataKeys.ngMetadata + "." + NGSecretManagerMetadataKeys.projectIdentifier;

  private final VaultEncryptorsRegistry vaultRegistry;
  private final KmsEncryptorsRegistry kmsRegistry;
  private final NGSecretManagerService ngSecretManagerService;
  private final WingsPersistence wingsPersistence;
  private final FileService fileService;
  private final SecretManagerConfigService secretManagerConfigService;
  private final GlobalEncryptDecryptClient globalEncryptDecryptClient;
  private final LocalSecretManagerService localSecretManagerService;

  private EncryptedData encrypt(
      @NotNull EncryptedData encryptedData, String secretValue, SecretManagerConfig secretManagerConfig) {
    EncryptedRecord encryptedRecord;
    switch (encryptedData.getEncryptionType()) {
      case VAULT:
        VaultEncryptor vaultEncryptor = vaultRegistry.getVaultEncryptor(secretManagerConfig.getEncryptionType());
        encryptedRecord = vaultEncryptor.createSecret(
            encryptedData.getAccountId(), encryptedData.getName(), secretValue, secretManagerConfig);
        break;
      case GCP_KMS:
      case KMS:
      case LOCAL:
        encryptedRecord = kmsRegistry.getKmsEncryptor(secretManagerConfig)
                              .encryptSecret(encryptedData.getAccountId(), secretValue, secretManagerConfig);
        break;
      default:
        throw new UnsupportedOperationException("Encryption type not supported: " + encryptedData.getEncryptionType());
    }
    return EncryptedData.builder()
        .encryptionKey(encryptedRecord.getEncryptionKey())
        .encryptedValue(encryptedRecord.getEncryptedValue())
        .build();
  }

  private String formNotFoundMessage(String baseMessage, String orgIdentifier, String projectIdentifier) {
    if (!StringUtils.isEmpty(orgIdentifier)) {
      baseMessage += String.format("in org: %s", orgIdentifier);
      if (!StringUtils.isEmpty(projectIdentifier)) {
        baseMessage += String.format(" and project: %s", projectIdentifier);
      }
    } else if (!StringUtils.isEmpty(projectIdentifier)) {
      baseMessage += "in project: %s" + projectIdentifier;
    } else {
      baseMessage += "in this scope.";
    }
    return baseMessage;
  }

  @Override
  public EncryptedData createSecretText(@NotNull SecretTextDTO dto) {
    // both value and path cannot be present, only one of them is allowed
    if (Optional.ofNullable(dto.getPath()).isPresent() && Optional.ofNullable(dto.getValue()).isPresent()) {
      throw new InvalidRequestException("Cannot provide both path and value while saving secret", USER);
    }

    // create encrypted data from dto
    EncryptedData data = EncryptedDataMapper.fromDTO(dto);
    NGEncryptedDataMetadata metadata = data.getNgMetadata();

    // check if secret with the same identifier is present in the same scope
    if (get(metadata.getAccountIdentifier(), metadata.getOrgIdentifier(), metadata.getProjectIdentifier(),
            metadata.getIdentifier())
            .isPresent()) {
      throw new SecretManagementException(ErrorCode.SECRET_MANAGEMENT_ERROR, "Duplicate secret present", USER);
    }

    // Fetch secret manager with which this secret will be saved
    Optional<SecretManagerConfig> secretManagerConfigOptional =
        ngSecretManagerService.get(metadata.getAccountIdentifier(), metadata.getOrgIdentifier(),
            metadata.getProjectIdentifier(), metadata.getSecretManagerIdentifier(), true);

    if (secretManagerConfigOptional.isPresent()) {
      SecretManagerConfig secretManagerConfig = secretManagerConfigOptional.get();

      if (isReadOnlySecretManager(secretManagerConfig)
          && (Inline.equals(dto.getValueType()) || Optional.ofNullable(dto.getValue()).isPresent())) {
        throw new SecretManagementException(
            SECRET_MANAGEMENT_ERROR, "Cannot create an Inline secret in read only secret manager", USER);
      }

      // validate format of path as per type of secret manager
      validatePath(data.getPath(), secretManagerConfig.getEncryptionType());

      // decrypt secrets (e.g. auth token etc.) before sending to delegate
      secretManagerConfigService.decryptEncryptionConfigSecrets(
          metadata.getAccountIdentifier(), secretManagerConfig, false);

      data.setKmsId(secretManagerConfig.getUuid());
      data.setEncryptionType(secretManagerConfig.getEncryptionType());
      data.getNgMetadata().setSecretManagerName(secretManagerConfig.getName());

      if (!StringUtils.isEmpty(dto.getValue())) {
        // send task to delegate for saving secret
        EncryptedData encryptedData = encrypt(data, dto.getValue(), secretManagerConfig);

        // set fields and save secret in DB
        data.setEncryptionKey(encryptedData.getEncryptionKey());
        data.setEncryptedValue(encryptedData.getEncryptedValue());
      }
      wingsPersistence.save(data);
      return data;
    } else {
      String message = "No such secret manager found";
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR,
          formNotFoundMessage(message, metadata.getOrgIdentifier(), metadata.getProjectIdentifier()), USER);
    }
  }

  @Override
  public PageResponse<EncryptedData> listSecrets(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, SettingVariableTypes type, String page, String size) {
    PageRequest<EncryptedData> pageRequest = new PageRequest<>();
    pageRequest.addFilter(ACCOUNT_IDENTIFIER_KEY, EQ, accountIdentifier);
    pageRequest.addFilter(ORG_IDENTIFIER_KEY, EQ, orgIdentifier);
    pageRequest.addFilter(PROJECT_IDENTIFIER_KEY, EQ, projectIdentifier);
    if (Optional.ofNullable(type).isPresent()) {
      pageRequest.addFilter(EncryptedDataKeys.type, EQ, type);
    }
    pageRequest.setLimit(size);
    pageRequest.setOffset(page);
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
  public boolean updateSecretText(
      @NotNull String account, String org, String project, String identifier, SecretTextUpdateDTO dto) {
    // while updating a secret, only one of value/path can be provided
    if (Optional.ofNullable(dto.getPath()).isPresent() && Optional.ofNullable(dto.getValue()).isPresent()) {
      throw new InvalidRequestException("Cannot provide both path and value while saving secret", USER);
    }

    // get existing secret text
    Optional<EncryptedData> encryptedDataOptional = get(account, org, project, identifier);

    if (encryptedDataOptional.isPresent()) {
      EncryptedData encryptedData = encryptedDataOptional.get();
      NGEncryptedDataMetadata metadata = encryptedData.getNgMetadata();

      // get secret manager with which secret text was encrypted
      Optional<SecretManagerConfig> secretManagerConfigOptional =
          ngSecretManagerService.get(metadata.getAccountIdentifier(), metadata.getOrgIdentifier(),
              metadata.getProjectIdentifier(), metadata.getSecretManagerIdentifier(), true);

      if (secretManagerConfigOptional.isPresent()) {
        if (isReadOnlySecretManager(secretManagerConfigOptional.get())
            && (Inline.equals(dto.getValueType()) || Optional.ofNullable(dto.getValue()).isPresent())) {
          throw new SecretManagementException(
              SECRET_MANAGEMENT_ERROR, "Cannot update to an Inline secret in read only secret manager", USER);
        }
        validatePath(dto.getPath(), secretManagerConfigOptional.get().getEncryptionType());

        // decrypt secret fields of secret manager
        secretManagerConfigService.decryptEncryptionConfigSecrets(account, secretManagerConfigOptional.get(), false);

        // if name has been changed, delete old text if it was created inline
        if (!encryptedData.getName().equals(dto.getName())
            && !Optional.ofNullable(encryptedData.getPath()).isPresent()) {
          deleteSecretInSecretManager(account, encryptedData, secretManagerConfigOptional.get());
        }

        // set updated values
        encryptedData.setName(dto.getName());
        encryptedData.setPath(dto.getPath());
        encryptedData.getNgMetadata().setTags(dto.getTags());
        encryptedData.getNgMetadata().setDescription(dto.getDescription());
        encryptedData.getNgMetadata().setDraft(dto.isDraft());

        // send to delegate to create/update secret
        if (!StringUtils.isEmpty(dto.getValue())) {
          EncryptedData updatedEncryptedData =
              encrypt(encryptedData, dto.getValue(), secretManagerConfigOptional.get());

          // set encryption key and value and save secret in DB
          encryptedData.setEncryptionKey(updatedEncryptedData.getEncryptionKey());
          encryptedData.setEncryptedValue(updatedEncryptedData.getEncryptedValue());
        }
        wingsPersistence.save(encryptedData);
        return true;
      } else {
        throw new InvalidRequestException(formNotFoundMessage("No such secret manager found",
                                              metadata.getOrgIdentifier(), metadata.getProjectIdentifier()),
            INVALID_REQUEST, USER);
      }
    }
    throw new InvalidRequestException(formNotFoundMessage("No such secret found", org, project), INVALID_REQUEST, USER);
  }

  @Override
  public boolean deleteSecretText(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    // Get secret text to delete from DB
    Optional<EncryptedData> encryptedDataOptional =
        get(accountIdentifier, orgIdentifier, projectIdentifier, identifier);

    if (encryptedDataOptional.isPresent()) {
      EncryptedData encryptedData = encryptedDataOptional.get();
      NGEncryptedDataMetadata metadata = encryptedData.getNgMetadata();

      // Get secret manager with which it was encrypted
      Optional<SecretManagerConfig> secretManagerConfigOptional = ngSecretManagerService.get(
          accountIdentifier, orgIdentifier, projectIdentifier, metadata.getSecretManagerIdentifier(), true);
      if (secretManagerConfigOptional.isPresent()) {
        if (isReadOnlySecretManager(secretManagerConfigOptional.get())
            && Optional.ofNullable(encryptedData.getEncryptedValue()).isPresent()) {
          throw new SecretManagementException(
              SECRET_MANAGEMENT_ERROR, "Cannot delete an Inline secret in read only secret manager", USER);
        }
        // if  secret text was created inline (not referenced), delete the secret in secret manager also
        if (!Optional.ofNullable(encryptedData.getPath()).isPresent()) {
          deleteSecretInSecretManager(accountIdentifier, encryptedData, secretManagerConfigOptional.get());
        }
        if (encryptedData.getType() == SettingVariableTypes.CONFIG_FILE) {
          switch (secretManagerConfigOptional.get().getEncryptionType()) {
            case LOCAL:
            case GCP_KMS:
            case KMS:
              fileService.deleteFile(String.valueOf(encryptedData.getEncryptedValue()), CONFIGS);
              break;
            default:
          }
        }
        // delete secret text finally in db
        return wingsPersistence.delete(EncryptedData.class, encryptedData.getUuid());
      }
    }
    throw new InvalidRequestException("No such secret found", INVALID_REQUEST, USER);
  }

  public void deleteSecretInSecretManager(
      String accountIdentifier, EncryptedData encryptedData, SecretManagerConfig secretManagerConfig) {
    switch (secretManagerConfig.getEncryptionType()) {
      case VAULT:
        vaultRegistry.getVaultEncryptor(secretManagerConfig.getEncryptionType())
            .deleteSecret(accountIdentifier, encryptedData, secretManagerConfig);
        return;
      case LOCAL:
      case GCP_KMS:
      case KMS:
        return;
      default:
        throw new UnsupportedOperationException(
            "Encryption type " + secretManagerConfig.getEncryptionType() + " is not supported in next gen");
    }
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

  public void setEncryptedValueToFileContent(EncryptedData encryptedData) {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    fileService.downloadToStream(String.valueOf(encryptedData.getEncryptedValue()), os, CONFIGS);
    encryptedData.setEncryptedValue(CHARSET.decode(ByteBuffer.wrap(os.toByteArray())).array());
  }

  @Override
  public List<EncryptedDataDetail> getEncryptionDetails(NGAccess ngAccess, DecryptableEntity object) {
    // if object is already decrypted, return empty list
    if (object.isDecrypted()) {
      return Collections.emptyList();
    }
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    List<Field> encryptedFields = object.getSecretReferenceFields();

    // iterate over all the fields with @SecretReference annotation
    for (Field field : encryptedFields) {
      try {
        field.setAccessible(true);

        // type cast the field to SecretRefData, if the type casted value is not present, continue
        SecretRefData secretRefData = (SecretRefData) field.get(object);
        if (!Optional.ofNullable(secretRefData).isPresent()) {
          continue;
        }
        String secretIdentifier = secretRefData.getIdentifier();
        Scope secretScope = secretRefData.getScope();

        // if sufficient information is there to process this field, try to process it
        if (Optional.ofNullable(secretIdentifier).isPresent() && Optional.ofNullable(secretScope).isPresent()) {
          String accountIdentifier = ngAccess.getAccountIdentifier();
          String orgIdentifier = getOrgIdentifier(ngAccess.getOrgIdentifier(), secretScope);
          String projectIdentifier = getProjectIdentifier(ngAccess.getProjectIdentifier(), secretScope);

          // get encrypted data from DB
          Optional<EncryptedData> encryptedDataOptional =
              get(accountIdentifier, orgIdentifier, projectIdentifier, secretIdentifier);
          if (encryptedDataOptional.isPresent()
              && Optional.ofNullable(encryptedDataOptional.get().getNgMetadata()).isPresent()) {
            EncryptedData encryptedData = encryptedDataOptional.get();

            // if type is file and file is saved elsewhere, download and save contents in encryptedValue
            if (encryptedData.getType() == SettingVariableTypes.CONFIG_FILE
                && ENCRYPTION_TYPES_REQUIRING_FILE_DOWNLOAD.contains(encryptedData.getEncryptionType())) {
              setEncryptedValueToFileContent(encryptedData);
            }

            // get secret manager with which this was secret was encrypted
            Optional<SecretManagerConfig> secretManagerConfigOptional = ngSecretManagerService.get(accountIdentifier,
                orgIdentifier, projectIdentifier, encryptedData.getNgMetadata().getSecretManagerIdentifier(), true);
            if (secretManagerConfigOptional.isPresent()) {
              SecretManagerConfig encryptionConfig = secretManagerConfigOptional.get();

              // decrypt secret fields of secret manager
              secretManagerConfigService.decryptEncryptionConfigSecrets(accountIdentifier, encryptionConfig, false);
              EncryptedRecordData encryptedRecordData;
              if (encryptionConfig.isGlobalKms()) {
                encryptedRecordData = globalEncryptDecryptClient.convertEncryptedRecordToLocallyEncrypted(
                    encryptedData, accountIdentifier, encryptionConfig);
                if (LOCAL.equals(encryptedRecordData.getEncryptionType())) {
                  encryptionConfig = localSecretManagerService.getEncryptionConfig(accountIdentifier);
                } else {
                  log.error("Failed to decrypt secret {} with global {} secret manager", encryptedData.getUuid(),
                      encryptionConfig.getEncryptionType());
                  continue;
                }
              } else {
                encryptedRecordData = SecretManager.buildRecordData(encryptedData);
              }
              encryptedDataDetails.add(EncryptedDataDetail.builder()
                                           .encryptedData(encryptedRecordData)
                                           .encryptionConfig(encryptionConfig)
                                           .fieldName(field.getName())
                                           .build());
            }
          }
        }
      } catch (IllegalAccessException exception) {
        throw new SecretManagementException(ENCRYPT_DECRYPT_ERROR, exception, USER);
      }
    }
    return encryptedDataDetails;
  }

  private Query<EncryptedData> getSearchQuery(@NotNull String accountIdentifier, String orgIdentifier,
      String projectIdentifier, SettingVariableTypes type, @NotNull String searchTerm) {
    // A search is always scoped, so we always set account, org and project in query
    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class)
                                     .field(ACCOUNT_IDENTIFIER_KEY)
                                     .equal(accountIdentifier)
                                     .field(ORG_IDENTIFIER_KEY)
                                     .equal(orgIdentifier)
                                     .field(PROJECT_IDENTIFIER_KEY)
                                     .equal(projectIdentifier);
    // type is optional, set if it is present
    if (Optional.ofNullable(type).isPresent()) {
      query = query.field(EncryptedDataKeys.type).equal(type);
    }
    // search term is mandatory, we search on name, identifier and tags fields
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

  private void validatePath(String path, EncryptionType encryptionType) {
    if (path != null && encryptionType == VAULT && path.indexOf('#') < 0) {
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR,
          "Secret path need to include the # sign with the the key name after. E.g. /foo/bar/my-secret#my-key.", USER);
    }
  }
}
