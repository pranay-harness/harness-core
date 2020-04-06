package software.wings.security.encryption.migration.secretparents.migrators;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.encryption.EncryptionReflectUtils;
import io.harness.exception.UnexpectedException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.SecretManagerConfig;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedDataParent;
import software.wings.security.encryption.migration.secretparents.SecretsMigrator;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

@Slf4j
@Singleton
public class SecretManagerConfigMigrator implements SecretsMigrator<SecretManagerConfig> {
  private WingsPersistence wingsPersistence;

  @Inject
  SecretManagerConfigMigrator(WingsPersistence wingsPersistence) {
    this.wingsPersistence = wingsPersistence;
  }

  @Override
  public List<SecretManagerConfig> getParents(Set<String> parentIds) {
    return parentIds.stream()
        .map(parentId -> wingsPersistence.get(SecretManagerConfig.class, parentId))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  public Optional<EncryptedDataParent> buildEncryptedDataParent(
      @NotNull String secretId, @NotNull SecretManagerConfig parent) {
    return fetchFieldReferringSecret(parent, secretId)
        .flatMap(fieldName
            -> Optional.of(new EncryptedDataParent(
                parent.getUuid(), SettingVariableTypes.valueOf(parent.getEncryptionType().name()), fieldName)));
  }

  private Optional<String> fetchFieldReferringSecret(
      @NotNull SecretManagerConfig secretManagerConfig, @NotNull String secretId) {
    List<Field> encryptedFieldList = EncryptionReflectUtils.getEncryptedFields(secretManagerConfig.getClass());
    try {
      for (Field encryptedField : encryptedFieldList) {
        encryptedField.setAccessible(true);
        String encryptedId;
        if (char[].class.equals(encryptedField.getType())) {
          encryptedId = String.copyValueOf((char[]) encryptedField.get(secretManagerConfig));
        } else {
          encryptedId = (String) encryptedField.get(secretManagerConfig);
        }
        if (isNotEmpty(encryptedId) && encryptedId.equals(secretId)) {
          return Optional.of(EncryptionReflectUtils.getEncryptedFieldTag(encryptedField));
        }
      }
      throw new UnexpectedException(
          "Cannot migrate the parent, the secret is not referred in any of the encrypted fields.");
    } catch (RuntimeException | IllegalAccessException e) {
      logger.warn("Could not migrate the parent {} for secret {}, due to this error", secretManagerConfig.getUuid(),
          secretId, e);
    }
    return Optional.empty();
  }
}
