package software.wings.utils;

import static io.harness.encryption.EncryptionReflectUtils.getEncryptedFieldTag;
import static io.harness.encryption.EncryptionReflectUtils.getEncryptedRefField;

import com.google.common.collect.Sets;

import io.harness.beans.Encryptable;
import io.harness.beans.EncryptedDataParent;
import io.harness.reflection.ReflectionUtils;
import io.harness.security.encryption.EncryptionType;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import software.wings.annotation.EncryptableSetting;
import software.wings.security.encryption.SecretParentsUpdateDetail;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@UtilityClass
public class WingsReflectionUtils {
  public static boolean isSetByYaml(Encryptable object, Field encryptedField) throws IllegalAccessException {
    encryptedField.setAccessible(true);
    String encryptedFieldValue = (String) encryptedField.get(object);
    if (encryptedFieldValue != null) {
      return isSetByYaml(encryptedFieldValue);
    }
    return false;
  }

  public static boolean isSetByYaml(@NonNull String secretId) {
    for (EncryptionType encryptionType : EncryptionType.values()) {
      if (secretId.startsWith(encryptionType.getYamlName())) {
        return true;
      }
    }
    return false;
  }

  public static Optional<EncryptableSetting> getEncryptableSetting(@NonNull Object object) {
    if (EncryptableSetting.class.isAssignableFrom(object.getClass())) {
      return Optional.of((EncryptableSetting) object);
    } else {
      for (Field field : object.getClass().getDeclaredFields()) {
        Object value = ReflectionUtils.getFieldValue(object, field);
        if (value instanceof EncryptableSetting) {
          return Optional.of((EncryptableSetting) value);
        }
      }
    }
    return Optional.empty();
  }

  public static Map<String, Set<EncryptedDataParent>> buildSecretIdsToParentsMap(
      @NonNull EncryptableSetting object, @NonNull String parentId) throws IllegalAccessException {
    Map<String, Set<EncryptedDataParent>> secretIdsToParentsMap = new HashMap<>();
    List<Field> encryptedFields = Optional.ofNullable(object.getEncryptedFields()).orElseGet(Collections::emptyList);
    for (Field encryptedField : encryptedFields) {
      Field encryptedRefField = getEncryptedRefField(encryptedField, object);
      encryptedRefField.setAccessible(true);
      Optional<String> secretIdOptional = Optional.ofNullable((String) encryptedRefField.get(object));
      secretIdOptional.ifPresent(secretId -> {
        Set<EncryptedDataParent> parents =
            Optional.ofNullable(secretIdsToParentsMap.get(secretId)).orElseGet(HashSet::new);
        String fieldKey = getEncryptedFieldTag(encryptedField);
        parents.add(new EncryptedDataParent(parentId, object.getSettingType(), fieldKey));
        secretIdsToParentsMap.put(secretId, parents);
      });
    }
    return secretIdsToParentsMap;
  }

  public static List<SecretParentsUpdateDetail> fetchSecretParentsUpdateDetailList(
      @NonNull Map<String, Set<EncryptedDataParent>> previousParentsMap,
      @NonNull Map<String, Set<EncryptedDataParent>> currentParentsMap) {
    Set<String> secretIds = new HashSet<>(previousParentsMap.keySet());
    secretIds.addAll(currentParentsMap.keySet());

    return secretIds.stream()
        .map(secretId -> {
          Set<EncryptedDataParent> previousParents =
              Optional.ofNullable(previousParentsMap.get(secretId)).orElse(new HashSet<>());
          Set<EncryptedDataParent> currentParents =
              Optional.ofNullable(currentParentsMap.get(secretId)).orElse(new HashSet<>());
          Set<EncryptedDataParent> parentsToRemove = Sets.difference(previousParents, currentParents);
          Set<EncryptedDataParent> parentsToAdd = Sets.difference(currentParents, previousParents);
          if (!parentsToRemove.isEmpty() || !parentsToAdd.isEmpty()) {
            return new SecretParentsUpdateDetail(secretId, parentsToAdd, parentsToRemove);
          }
          return null;
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }
}
