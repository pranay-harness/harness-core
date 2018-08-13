package software.wings.annotation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.WingsReflectionUtils;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Created by mike@ on 4/25/17.
 */
public interface Encryptable {
  String getAccountId();

  void setAccountId(String accountId);

  @SchemaIgnore SettingVariableTypes getSettingType();

  @JsonIgnore
  @SchemaIgnore
  default List<Field> getEncryptedFields() {
    return WingsReflectionUtils.getEncryptedFields(this.getClass());
  }

  @JsonIgnore
  @SchemaIgnore
  default boolean isDecrypted() {
    return false;
  }

  default void setDecrypted(boolean decrypted) {}
}
