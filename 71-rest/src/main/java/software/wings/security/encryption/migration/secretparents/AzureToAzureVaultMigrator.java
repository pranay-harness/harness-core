package software.wings.security.encryption.migration.secretparents;

import static software.wings.settings.SettingValue.SettingVariableTypes.AZURE;
import static software.wings.settings.SettingValue.SettingVariableTypes.AZURE_VAULT;

import com.google.inject.Inject;

import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.SecretManagerConfig;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedData.EncryptedDataKeys;
import software.wings.security.encryption.EncryptedDataParent;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

@Slf4j
public class AzureToAzureVaultMigrator {
  private WingsPersistence wingsPersistence;

  @Inject
  public AzureToAzureVaultMigrator(WingsPersistence wingsPersistence) {
    this.wingsPersistence = wingsPersistence;
  }

  public boolean shouldConvertToAzureVaultType(@NotNull EncryptedData encryptedData) {
    if (encryptedData.getType() != AZURE) {
      return false;
    }
    Set<String> parentIds =
        encryptedData.getParents().stream().map(EncryptedDataParent::getId).collect(Collectors.toSet());
    return parentIds.stream().anyMatch(parentId -> {
      SecretManagerConfig secretManagerConfig = wingsPersistence.get(SecretManagerConfig.class, parentId);
      return secretManagerConfig != null;
    });
  }

  public Optional<EncryptedData> convertToAzureVaultSettingType(@NotNull EncryptedData encryptedData) {
    UpdateOperations<EncryptedData> encryptedDataUpdateOperations =
        wingsPersistence.createUpdateOperations(EncryptedData.class).set(EncryptedDataKeys.type, AZURE_VAULT);

    Query<EncryptedData> encryptedDataQuery = wingsPersistence.createQuery(EncryptedData.class)
                                                  .field(EncryptedData.ID_KEY)
                                                  .equal(encryptedData.getUuid())
                                                  .field(EncryptedData.LAST_UPDATED_AT_KEY)
                                                  .equal(encryptedData.getLastUpdatedAt())
                                                  .field(EncryptedDataKeys.type)
                                                  .equal(AZURE);

    return Optional.ofNullable(wingsPersistence.findAndModify(
        encryptedDataQuery, encryptedDataUpdateOperations, HPersistence.returnNewOptions));
  }
}
