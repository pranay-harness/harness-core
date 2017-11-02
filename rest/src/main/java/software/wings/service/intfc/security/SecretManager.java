package software.wings.service.intfc.security;

import software.wings.security.encryption.SecretChangeLog;
import software.wings.security.encryption.SecretUsageLog;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.List;

/**
 * Created by rsingh on 10/30/17.
 */
public interface SecretManager {
  List<SecretUsageLog> getUsageLogs(final String entityId, SettingValue.SettingVariableTypes variableType)
      throws IllegalAccessException;

  List<SecretChangeLog> getChangeLogs(String entityId, SettingVariableTypes variableType) throws IllegalAccessException;
}
