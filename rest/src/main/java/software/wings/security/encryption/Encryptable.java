package software.wings.security.encryption;

import software.wings.settings.SettingValue.SettingVariableTypes;

/**
 * Created by mike@ on 4/25/17.
 */
public interface Encryptable {
  String getAccountId();

  void setAccountId(String accountId);

  SettingVariableTypes getSettingType();
}
