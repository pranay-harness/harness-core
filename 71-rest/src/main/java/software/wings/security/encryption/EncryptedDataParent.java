package software.wings.security.encryption;

import io.harness.encryption.EncryptionReflectUtils;
import io.harness.exception.UnexpectedException;
import io.harness.reflection.ReflectionUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.lang.reflect.Field;
import javax.validation.constraints.NotNull;

@Value
@Builder
@AllArgsConstructor
@EqualsAndHashCode
@FieldNameConstants(innerTypeName = "EncryptedDataParentKeys")
public class EncryptedDataParent {
  String id;
  SettingVariableTypes type;
  String fieldName;

  public static EncryptedDataParent createParentRef(@NotNull String objectId, @NotNull Class objectClass,
      @NotNull String fieldName, @NotNull SettingVariableTypes type) {
    Field encryptedField = ReflectionUtils.getFieldByName(objectClass, fieldName);
    if (encryptedField == null) {
      throw new UnexpectedException(String.format("Field %s does not exist in the %s", fieldName, objectClass));
    }
    String fieldKey = EncryptionReflectUtils.getEncryptedFieldTag(encryptedField);
    return new EncryptedDataParent(objectId, type, fieldKey);
  }
}
