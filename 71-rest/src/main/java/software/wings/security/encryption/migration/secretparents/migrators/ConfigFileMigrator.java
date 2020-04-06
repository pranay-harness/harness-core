package software.wings.security.encryption.migration.secretparents.migrators;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.UnexpectedException;
import io.harness.reflection.ReflectionUtils;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.ConfigFile;
import software.wings.beans.ConfigFile.ConfigFileKeys;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedDataParent;
import software.wings.security.encryption.migration.secretparents.SecretsMigrator;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

@Slf4j
@Singleton
public class ConfigFileMigrator implements SecretsMigrator<ConfigFile> {
  private WingsPersistence wingsPersistence;

  @Inject
  public ConfigFileMigrator(WingsPersistence wingsPersistence) {
    this.wingsPersistence = wingsPersistence;
  }

  @Override
  public List<ConfigFile> getParents(Set<String> parentIds) {
    return parentIds.stream()
        .map(parentId -> wingsPersistence.get(ConfigFile.class, parentId))
        .filter(Objects::nonNull)
        .filter(ConfigFile::isEncrypted)
        .collect(Collectors.toList());
  }

  public Optional<EncryptedDataParent> buildEncryptedDataParent(@NotNull String secretId, @NotNull ConfigFile parent) {
    return fetchFieldReferringSecret(parent, secretId)
        .flatMap(
            fieldName -> Optional.of(new EncryptedDataParent(parent.getUuid(), parent.getSettingType(), fieldName)));
  }

  private Optional<String> fetchFieldReferringSecret(@NotNull ConfigFile configFile, @NotNull String secretId) {
    try {
      Field encryptedField = ReflectionUtils.getFieldByName(configFile.getClass(), ConfigFileKeys.encryptedFileId);
      if (encryptedField != null) {
        encryptedField.setAccessible(true);
        String encryptedId = (String) encryptedField.get(configFile);
        if (isNotEmpty(encryptedId) && encryptedId.equals(secretId)) {
          return Optional.of(configFile.getSettingType().toString());
        }
      }
      throw new UnexpectedException("Cannot migrate the parent, the secret is not referred in the encrypted file id.");
    } catch (RuntimeException | IllegalAccessException e) {
      logger.warn(
          "Could not migrate the parent {} for secret {}, due to this error", configFile.getUuid(), secretId, e);
    }
    return Optional.empty();
  }
}