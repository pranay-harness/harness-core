package software.wings.security.encryption.migration.secretparents.migrators;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.encryption.EncryptionReflectUtils;
import io.harness.exception.UnexpectedException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.ServiceVariable;
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
public class ServiceVariableMigrator implements SecretsMigrator<ServiceVariable> {
  private WingsPersistence wingsPersistence;

  @Inject
  public ServiceVariableMigrator(WingsPersistence wingsPersistence) {
    this.wingsPersistence = wingsPersistence;
  }

  @Override
  public List<ServiceVariable> getParents(Set<String> parentIds) {
    return parentIds.stream()
        .map(parentId -> wingsPersistence.get(ServiceVariable.class, parentId))
        .filter(Objects::nonNull)
        .filter(serviceVariable -> serviceVariable.getType() == ServiceVariable.Type.ENCRYPTED_TEXT)
        .collect(Collectors.toList());
  }

  public Optional<EncryptedDataParent> buildEncryptedDataParent(
      @NotNull String secretId, @NotNull ServiceVariable parent) {
    return fetchFieldReferringSecret(parent, secretId)
        .flatMap(
            fieldName -> Optional.of(new EncryptedDataParent(parent.getUuid(), parent.getSettingType(), fieldName)));
  }

  private Optional<String> fetchFieldReferringSecret(
      @NotNull ServiceVariable serviceVariable, @NotNull String secretId) {
    List<Field> encryptedFieldList = serviceVariable.getEncryptedFields();
    try {
      for (Field encryptedField : encryptedFieldList) {
        encryptedField.setAccessible(true);
        Field encryptedRefField = EncryptionReflectUtils.getEncryptedRefField(encryptedField, serviceVariable);
        encryptedRefField.setAccessible(true);
        String encryptedId = (String) encryptedRefField.get(serviceVariable);
        if (isNotEmpty(encryptedId) && encryptedId.equals(secretId)) {
          return Optional.of(EncryptionReflectUtils.getEncryptedFieldTag(encryptedField));
        }
      }
      throw new UnexpectedException(
          "Cannot migrate the parent, the secret is not referred in any of the encrypted fields.");
    } catch (RuntimeException | IllegalAccessException e) {
      logger.warn(
          "Could not migrate the parent {} for secret {}, due to this error", serviceVariable.getUuid(), secretId, e);
    }
    return Optional.empty();
  }
}
