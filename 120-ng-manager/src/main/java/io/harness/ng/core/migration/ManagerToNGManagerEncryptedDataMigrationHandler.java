package io.harness.ng.core.migration;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static io.harness.remote.client.RestClientUtils.getResponse;
import static io.harness.security.encryption.EncryptionType.GCP_KMS;
import static io.harness.security.encryption.EncryptionType.KMS;
import static io.harness.security.encryption.EncryptionType.LOCAL;

import static java.time.Duration.ofSeconds;

import io.harness.annotations.dev.OwnedBy;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.mongo.iterator.filter.SpringFilterExpander;
import io.harness.mongo.iterator.provider.SpringPersistenceProvider;
import io.harness.ng.core.dao.NGEncryptedDataDao;
import io.harness.ng.core.entities.NGEncryptedData;
import io.harness.ng.core.models.Secret;
import io.harness.ng.core.models.Secret.SecretKeys;
import io.harness.repositories.ng.core.spring.SecretRepository;
import io.harness.secretmanagerclient.dto.EncryptedDataMigrationDTO;
import io.harness.secretmanagerclient.remote.SecretManagerClient;
import io.harness.secrets.SecretsFileService;
import io.harness.security.encryption.EncryptionType;

import software.wings.settings.SettingVariableTypes;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
@Singleton
@Slf4j
public class ManagerToNGManagerEncryptedDataMigrationHandler implements Handler<Secret> {
  private static final Set<EncryptionType> ENCRYPTION_TYPES_REQUIRING_FILE_DOWNLOAD = EnumSet.of(LOCAL, GCP_KMS, KMS);
  private final PersistenceIteratorFactory persistenceIteratorFactory;
  private final MongoTemplate mongoTemplate;
  private final SecretManagerClient secretManagerClient;
  private final NGEncryptedDataDao encryptedDataDao;
  private final SecretsFileService secretsFileService;
  private final SecretRepository secretRepository;
  private final boolean ngSecretMigrationCompleted;

  @Inject
  public ManagerToNGManagerEncryptedDataMigrationHandler(PersistenceIteratorFactory persistenceIteratorFactory,
      MongoTemplate mongoTemplate, SecretManagerClient secretManagerClient, NGEncryptedDataDao encryptedDataDao,
      SecretsFileService secretsFileService, SecretRepository secretRepository,
      @Named("ngSecretMigrationCompleted") boolean ngSecretMigrationCompleted) {
    this.persistenceIteratorFactory = persistenceIteratorFactory;
    this.mongoTemplate = mongoTemplate;
    this.secretManagerClient = secretManagerClient;
    this.encryptedDataDao = encryptedDataDao;
    this.secretsFileService = secretsFileService;
    this.secretRepository = secretRepository;
    this.ngSecretMigrationCompleted = ngSecretMigrationCompleted;
  }

  public void registerIterators() {
    if (ngSecretMigrationCompleted) {
      return;
    }
    SpringFilterExpander filterExpander = getFilterQuery();
    registerIteratorWithFactory(filterExpander);
  }

  private void registerIteratorWithFactory(@NotNull SpringFilterExpander filterExpander) {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PersistenceIteratorFactory.PumpExecutorOptions.builder()
            .name(this.getClass().getName())
            .poolSize(3)
            .interval(ofSeconds(5))
            .build(),
        Secret.class,
        MongoPersistenceIterator.<Secret, SpringFilterExpander>builder()
            .clazz(Secret.class)
            .fieldName(SecretKeys.nextIteration)
            .targetInterval(ofSeconds(10))
            .acceptableExecutionTime(ofSeconds(20))
            .acceptableNoAlertDelay(ofSeconds(60))
            .filterExpander(filterExpander)
            .handler(this)
            .schedulingType(REGULAR)
            .persistenceProvider(new SpringPersistenceProvider<>(mongoTemplate))
            .redistribute(true));
  }

  private SpringFilterExpander getFilterQuery() {
    return query -> query.addCriteria(Criteria.where(SecretKeys.migratedFromManager).ne(true));
  }

  @Override
  public void handle(Secret secret) {
    NGEncryptedData encryptedData = encryptedDataDao.get(secret.getAccountIdentifier(), secret.getOrgIdentifier(),
        secret.getProjectIdentifier(), secret.getIdentifier());
    if (encryptedData == null) {
      encryptedData = fromEncryptedDataMigrationDTO(
          getResponse(secretManagerClient.getEncryptedDataMigrationDTO(secret.getIdentifier(),
              secret.getAccountIdentifier(), secret.getOrgIdentifier(), secret.getProjectIdentifier())));
      if (encryptedData == null) {
        log.info(String.format(
            "Secret with accountIdentifier: %s, orgIdentifier: %s, projectIdentifier: %s and identifier: %s could not be migrated from manager because Encrypted Data not found",
            secret.getAccountIdentifier(), secret.getOrgIdentifier(), secret.getProjectIdentifier(),
            secret.getIdentifier()));
        return;
      }
      if (encryptedData.getType() == SettingVariableTypes.CONFIG_FILE
          && ENCRYPTION_TYPES_REQUIRING_FILE_DOWNLOAD.contains(encryptedData.getEncryptionType())
          && Optional.ofNullable(encryptedData.getEncryptedValue()).isPresent()) {
        String encryptedFileId = secretsFileService.createFile(
            secret.getName(), secret.getAccountIdentifier(), encryptedData.getEncryptedValue());
        encryptedData.setEncryptedValue(encryptedFileId == null ? null : encryptedFileId.toCharArray());
      }
      encryptedData.setId(null);
      encryptedDataDao.save(encryptedData);
    }
    getResponse(secretManagerClient.deleteAfterMigration(secret.getIdentifier(), secret.getAccountIdentifier(),
        secret.getOrgIdentifier(), secret.getProjectIdentifier()));
    secret.setMigratedFromManager(true);
    secretRepository.save(secret);
    log.info(String.format(
        "Secret with accountIdentifier: %s, orgIdentifier: %s, projectIdentifier: %s and identifier: %s successfully migrated from manager",
        secret.getAccountIdentifier(), secret.getOrgIdentifier(), secret.getProjectIdentifier(),
        secret.getIdentifier()));
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
