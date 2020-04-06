package software.wings.security.encryption.migration.secretparents;

import com.google.inject.Inject;

import io.harness.persistence.HPersistence;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.FeatureName;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedData.EncryptedDataKeys;
import software.wings.security.encryption.EncryptedDataParent;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

@Slf4j
public class EncryptedDataMigrator {
  private WingsPersistence wingsPersistence;
  private FeatureFlagService featureFlagService;
  private MigratorRegistry migratorRegistry;

  @Inject
  public EncryptedDataMigrator(
      WingsPersistence wingsPersistence, MigratorRegistry migratorRegistry, FeatureFlagService featureFlagService) {
    this.wingsPersistence = wingsPersistence;
    this.migratorRegistry = migratorRegistry;
    this.featureFlagService = featureFlagService;
  }

  public boolean shouldMigrate(EncryptedData encryptedData) {
    return !featureFlagService.isEnabled(FeatureName.SECRET_PARENTS_MIGRATED, encryptedData.getAccountId())
        && !encryptedData.areParentIdsEquivalentToParent();
  }

  public boolean canMigrate(EncryptedData encryptedData) {
    return migratorRegistry.getMigrator(encryptedData.getType()).isPresent();
  }

  public Optional<EncryptedData> migrateEncryptedDataParents(@NotNull EncryptedData encryptedData) {
    return migratorRegistry.getMigrator(encryptedData.getType())
        .flatMap(secretsMigrator -> migrateEncryptedDataInternal(encryptedData, secretsMigrator));
  }

  private <T extends PersistentEntity & UuidAware> Optional<EncryptedData> migrateEncryptedDataInternal(
      @NotNull EncryptedData encryptedData, @NotNull SecretsMigrator<T> secretsMigrator) {
    Set<String> parentIds =
        encryptedData.getParents().stream().map(EncryptedDataParent::getId).collect(Collectors.toSet());
    List<T> parents = secretsMigrator.getParents(parentIds);

    Set<EncryptedDataParent> encryptedDataParents =
        parents.stream()
            .map(parent -> secretsMigrator.buildEncryptedDataParent(encryptedData.getUuid(), parent))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toSet());

    Set<String> filteredParentIds =
        encryptedDataParents.stream().map(EncryptedDataParent::getId).collect(Collectors.toSet());

    UpdateOperations<EncryptedData> encryptedDataUpdateOperations =
        wingsPersistence.createUpdateOperations(EncryptedData.class)
            .set(EncryptedDataKeys.parentIds, filteredParentIds)
            .set(EncryptedDataKeys.parents, encryptedDataParents);

    if (encryptedData.getType() == SettingVariableTypes.YAML_GIT_SYNC) {
      encryptedDataUpdateOperations.set(EncryptedDataKeys.type, SettingVariableTypes.GIT);
    }

    Query<EncryptedData> encryptedDataQuery = wingsPersistence.createQuery(EncryptedData.class)
                                                  .field(EncryptedData.ID_KEY)
                                                  .equal(encryptedData.getUuid())
                                                  .field(EncryptedData.LAST_UPDATED_AT_KEY)
                                                  .equal(encryptedData.getLastUpdatedAt());

    logger.info("Updated secret {} with parentIds {} and parents {}", encryptedData.getUuid(), filteredParentIds,
        encryptedDataParents);

    return Optional.ofNullable(wingsPersistence.findAndModify(
        encryptedDataQuery, encryptedDataUpdateOperations, HPersistence.returnNewOptions));
  }
}
