package io.harness.ng.core.migration;

import static io.harness.NGConstants.HARNESS_SECRET_MANAGER_IDENTIFIER;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.remote.client.RestClientUtils.getResponse;
import static io.harness.secretmanagerclient.SecretType.SSHKey;
import static io.harness.secretmanagerclient.SecretType.SecretFile;
import static io.harness.secretmanagerclient.SecretType.SecretText;
import static io.harness.security.encryption.EncryptionType.GCP_KMS;
import static io.harness.security.encryption.EncryptionType.KMS;
import static io.harness.security.encryption.EncryptionType.LOCAL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.NGMigration;
import io.harness.ng.core.api.NGEncryptedDataService;
import io.harness.ng.core.dao.NGEncryptedDataDao;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.ng.core.dto.secrets.SecretFileSpecDTO;
import io.harness.ng.core.dto.secrets.SecretTextSpecDTO;
import io.harness.ng.core.entities.NGEncryptedData;
import io.harness.ng.core.models.Secret;
import io.harness.ng.core.models.Secret.SecretKeys;
import io.harness.ng.core.models.SecretFileSpec;
import io.harness.ng.core.models.SecretTextSpec;
import io.harness.repositories.ng.core.spring.SecretRepository;
import io.harness.secretmanagerclient.ValueType;
import io.harness.secretmanagerclient.dto.EncryptedDataMigrationDTO;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.secretmanagerclient.remote.SecretManagerClient;
import io.harness.secrets.SecretsFileService;
import io.harness.security.encryption.EncryptionType;
import io.harness.utils.RetryUtils;

import software.wings.settings.SettingVariableTypes;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@Slf4j
@OwnedBy(PL)
public class NGSecretMigrationFromManager implements NGMigration {
  private static final Set<EncryptionType> ENCRYPTION_TYPES_REQUIRING_FILE_DOWNLOAD = EnumSet.of(LOCAL, GCP_KMS, KMS);
  private final RetryPolicy<Object> retryPolicy = RetryUtils.getRetryPolicy(
      "[Retrying]: Failed migrating Secret; attempt: {}", "[Failed]: Failed migrating Secret; attempt: {}",
      ImmutableList.of(OptimisticLockingFailureException.class, DuplicateKeyException.class), Duration.ofSeconds(1), 3,
      log);

  private final MongoTemplate mongoTemplate;
  private final SecretManagerClient secretManagerClient;
  private final NGEncryptedDataDao encryptedDataDao;
  private final SecretsFileService secretsFileService;
  private final SecretRepository secretRepository;
  private final NGEncryptedDataService encryptedDataService;

  @Inject
  public NGSecretMigrationFromManager(MongoTemplate mongoTemplate, SecretManagerClient secretManagerClient,
      NGEncryptedDataDao encryptedDataDao, SecretsFileService secretsFileService, SecretRepository secretRepository,
      NGEncryptedDataService encryptedDataService) {
    this.mongoTemplate = mongoTemplate;
    this.secretManagerClient = secretManagerClient;
    this.encryptedDataDao = encryptedDataDao;
    this.secretsFileService = secretsFileService;
    this.secretRepository = secretRepository;
    this.encryptedDataService = encryptedDataService;
  }

  @Override
  public void migrate() {
    Criteria criteria = Criteria.where(SecretKeys.migratedFromManager).ne(Boolean.TRUE);
    List<Secret> secrets = mongoTemplate.find(new Query(criteria), Secret.class);
    secrets.forEach(this::handleWithCare);
  }

  private void handleWithCare(Secret secret) {
    try {
      Failsafe.with(retryPolicy).run(() -> handle(secret));
    } catch (Exception exception) {
      log.error(String.format(
          "Unexpected error occurred during migration of secret with account %s, org %s, project %s and identifier %s",
          secret.getAccountIdentifier(), secret.getOrgIdentifier(), secret.getProjectIdentifier(),
          secret.getIdentifier()));
    }
  }

  private void handle(Secret secret) {
    if (!SSHKey.equals(secret.getType())) {
      NGEncryptedData encryptedData = encryptedDataDao.get(secret.getAccountIdentifier(), secret.getOrgIdentifier(),
          secret.getProjectIdentifier(), secret.getIdentifier());
      String secretManagerIdentifier = getSecretManagerIdentifier(secret);
      if (encryptedData == null) {
        encryptedData = fromEncryptedDataMigrationDTO(getResponse(secretManagerClient.getEncryptedDataMigrationDTO(
            secret.getIdentifier(), secret.getAccountIdentifier(), secret.getOrgIdentifier(),
            secret.getProjectIdentifier(), HARNESS_SECRET_MANAGER_IDENTIFIER.equals(secretManagerIdentifier))));
        if (encryptedData == null) {
          encryptedData = getDummyEncryptedData(secret);
        }
        if (encryptedData == null) {
          log.info(String.format(
              "Secret with accountIdentifier: %s, orgIdentifier: %s, projectIdentifier: %s and identifier: %s could not be migrated because Encrypted Data entry not found in manager",
              secret.getAccountIdentifier(), secret.getOrgIdentifier(), secret.getProjectIdentifier(),
              secret.getIdentifier()));
          secret.setMigratedFromManager(true);
          secretRepository.save(secret);
          return;
        }
        if (!HARNESS_SECRET_MANAGER_IDENTIFIER.equals(secretManagerIdentifier)) {
          if (encryptedData.getType() == SettingVariableTypes.CONFIG_FILE
              && ENCRYPTION_TYPES_REQUIRING_FILE_DOWNLOAD.contains(encryptedData.getEncryptionType())
              && Optional.ofNullable(encryptedData.getEncryptedValue()).isPresent()) {
            String encryptedFileId = secretsFileService.createFile(
                secret.getName(), secret.getAccountIdentifier(), encryptedData.getEncryptedValue());
            encryptedData.setEncryptedValue(encryptedFileId == null ? null : encryptedFileId.toCharArray());
          }
          encryptedData.setId(null);
          encryptedDataDao.save(encryptedData);
        } else {
          if (encryptedData.getType() == SettingVariableTypes.CONFIG_FILE) {
            encryptedDataService.createSecretFile(secret.getAccountIdentifier(), buildSecretDTOV2(encryptedData),
                isEmpty(encryptedData.getEncryptedValue())
                    ? null
                    : new ByteArrayInputStream(String.valueOf(encryptedData.getEncryptedValue()).getBytes()));
          } else {
            encryptedDataService.createSecretText(secret.getAccountIdentifier(), buildSecretDTOV2(encryptedData));
          }
        }
      }
    }
    secret.setMigratedFromManager(true);
    secretRepository.save(secret);
    log.info(String.format(
        "Secret with accountIdentifier: %s, orgIdentifier: %s, projectIdentifier: %s and identifier: %s successfully migrated from manager",
        secret.getAccountIdentifier(), secret.getOrgIdentifier(), secret.getProjectIdentifier(),
        secret.getIdentifier()));
  }

  private SecretDTOV2 buildSecretDTOV2(NGEncryptedData encryptedData) {
    SecretDTOV2 secretDTOV2 = SecretDTOV2.builder()
                                  .orgIdentifier(encryptedData.getOrgIdentifier())
                                  .projectIdentifier(encryptedData.getProjectIdentifier())
                                  .identifier(encryptedData.getIdentifier())
                                  .name(encryptedData.getName())
                                  .build();
    if (encryptedData.getType() == SettingVariableTypes.CONFIG_FILE) {
      SecretFileSpecDTO specDTO =
          SecretFileSpecDTO.builder().secretManagerIdentifier(encryptedData.getSecretManagerIdentifier()).build();
      secretDTOV2.setSpec(specDTO);
      secretDTOV2.setType(SecretFile);
    } else {
      SecretTextSpecDTO specDTO;
      if (isEmpty(encryptedData.getPath())) {
        specDTO =
            SecretTextSpecDTO.builder()
                .value(isEmpty(encryptedData.getEncryptedValue()) ? null
                                                                  : String.valueOf(encryptedData.getEncryptedValue()))
                .valueType(ValueType.Inline)
                .secretManagerIdentifier(encryptedData.getSecretManagerIdentifier())
                .build();
      } else {
        specDTO =
            SecretTextSpecDTO.builder()
                .value(isEmpty(encryptedData.getEncryptedValue()) ? null
                                                                  : String.valueOf(encryptedData.getEncryptedValue()))
                .valueType(ValueType.Reference)
                .secretManagerIdentifier(encryptedData.getSecretManagerIdentifier())
                .build();
      }
      secretDTOV2.setSpec(specDTO);
      secretDTOV2.setType(SecretText);
    }
    return secretDTOV2;
  }

  private NGEncryptedData getDummyEncryptedData(Secret secret) {
    SecretManagerConfigDTO secretManagerConfigDTO =
        getResponse(secretManagerClient.getSecretManager(getSecretManagerIdentifier(secret),
            secret.getAccountIdentifier(), secret.getOrgIdentifier(), secret.getProjectIdentifier(), true));
    if (secretManagerConfigDTO == null) {
      return null;
    }
    NGEncryptedData encryptedData = NGEncryptedData.builder()
                                        .accountIdentifier(secret.getAccountIdentifier())
                                        .orgIdentifier(secret.getOrgIdentifier())
                                        .projectIdentifier(secret.getProjectIdentifier())
                                        .identifier(secret.getIdentifier())
                                        .name(secret.getName())
                                        .encryptionType(secretManagerConfigDTO.getEncryptionType())
                                        .secretManagerIdentifier(getSecretManagerIdentifier(secret))
                                        .build();
    if (SecretFile.equals(secret.getType())) {
      encryptedData.setBase64Encoded(true);
      encryptedData.setType(SettingVariableTypes.CONFIG_FILE);
    } else {
      encryptedData.setType(SettingVariableTypes.SECRET_TEXT);
    }
    return encryptedData;
  }

  private String getSecretManagerIdentifier(Secret secret) {
    if (SecretText.equals(secret.getType())) {
      return ((SecretTextSpec) secret.getSecretSpec()).getSecretManagerIdentifier();
    } else if (SecretFile.equals(secret.getType())) {
      return ((SecretFileSpec) secret.getSecretSpec()).getSecretManagerIdentifier();
    }
    return null;
  }

  public static NGEncryptedData fromEncryptedDataMigrationDTO(EncryptedDataMigrationDTO encryptedDataMigrationDTO) {
    if (encryptedDataMigrationDTO == null) {
      return null;
    }
    return NGEncryptedData.builder()
        .id(encryptedDataMigrationDTO.getUuid())
        .accountIdentifier(encryptedDataMigrationDTO.getAccountIdentifier())
        .orgIdentifier(encryptedDataMigrationDTO.getOrgIdentifier())
        .projectIdentifier(encryptedDataMigrationDTO.getProjectIdentifier())
        .identifier(encryptedDataMigrationDTO.getIdentifier())
        .name(encryptedDataMigrationDTO.getName())
        .encryptionType(encryptedDataMigrationDTO.getEncryptionType())
        .secretManagerIdentifier(encryptedDataMigrationDTO.getKmsId())
        .encryptionKey(encryptedDataMigrationDTO.getEncryptionKey())
        .encryptedValue(encryptedDataMigrationDTO.getEncryptedValue())
        .path(encryptedDataMigrationDTO.getPath())
        .base64Encoded(encryptedDataMigrationDTO.isBase64Encoded())
        .type(encryptedDataMigrationDTO.getType())
        .build();
  }
}
