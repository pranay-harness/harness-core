package software.wings.security.encryption.migration.secretparents.migrators;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.Encryptable;
import io.harness.encryption.EncryptionReflectUtils;
import io.harness.exception.UnexpectedException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.SettingAttribute;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedDataParent;
import software.wings.security.encryption.migration.secretparents.SecretsMigrator;
import software.wings.settings.SettingValue;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

@Slf4j
@Singleton
public class SettingAttributeMigrator implements SecretsMigrator<SettingAttribute> {
  @Inject private WingsPersistence wingsPersistence;

  public List<SettingAttribute> getParents(@NotNull Set<String> parentIds) {
    return parentIds.stream()
        .map(parentId -> wingsPersistence.get(SettingAttribute.class, parentId))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  public Optional<EncryptedDataParent> buildEncryptedDataParent(
      @NotNull String secretId, @NotNull SettingAttribute parent) {
    SettingValue settingValue = parent.getValue();
    return fetchFieldReferringSecret(settingValue, secretId, parent.getUuid())
        .flatMap(fieldName
            -> Optional.of(new EncryptedDataParent(parent.getUuid(), parent.getValue().getSettingType(), fieldName)));
  }

  private Optional<String> fetchFieldReferringSecret(
      @NotNull SettingValue settingValue, @NotNull String secretId, @NotNull String parentId) {
    List<Field> encryptedFieldList = settingValue.getEncryptedFields();
    try {
      for (Field encryptedField : encryptedFieldList) {
        encryptedField.setAccessible(true);
        Field encryptedRefField =
            EncryptionReflectUtils.getEncryptedRefField(encryptedField, (Encryptable) settingValue);
        encryptedRefField.setAccessible(true);
        String encryptedId = (String) encryptedRefField.get(settingValue);
        if (isNotEmpty(encryptedId) && encryptedId.equals(secretId)) {
          return Optional.of(EncryptionReflectUtils.getEncryptedFieldTag(encryptedField));
        }
      }
      throw new UnexpectedException(
          "Cannot migrate the parent, the secret is not referred in any of the encrypted fields.");
    } catch (RuntimeException | IllegalAccessException e) {
      logger.warn("Could not migrate the parent {} for secret {}, due to this error", parentId, secretId, e);
    }
    return Optional.empty();
  }
}
