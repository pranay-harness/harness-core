package software.wings.helpers.ext.vault;

import software.wings.settings.SettingValue.SettingVariableTypes;

import java.io.IOException;

/**
 * Created by rsingh on 11/3/17.
 */
public interface VaultRestClient {
  boolean writeSecret(String authToken, String basePath, String keyName, SettingVariableTypes settingType, String value)
      throws IOException;

  boolean deleteSecret(String authToken, String basePath, String path) throws IOException;

  String readSecret(String authToken, String basePath, String keyName) throws IOException;

  boolean renewToken(String authToken) throws IOException;
}
