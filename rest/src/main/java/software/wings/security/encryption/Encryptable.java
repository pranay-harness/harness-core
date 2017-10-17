package software.wings.security.encryption;

import software.wings.settings.SettingValue.SettingVariableTypes;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Created by mike@ on 4/25/17.
 */
public interface Encryptable {
  String getAccountId();

  void setAccountId(String accountId);

  SettingVariableTypes getSettingType();

  List<Field> getEncryptedFields();
}
