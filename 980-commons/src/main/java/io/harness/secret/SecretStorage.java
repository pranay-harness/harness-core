package io.harness.secret;

import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretVersionName;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SecretStorage {
  private SecretManagerServiceClient client;

  public String fetchSecret(String project, String secretId) throws IOException {
    try {
      SecretVersionName secretVersionName = SecretVersionName.of(project, secretId, "latest");
      AccessSecretVersionResponse response = client.accessSecretVersion(secretVersionName);
      return response.getPayload().getData().toStringUtf8();
    } catch (Exception e) {
      log.warn(String.format("Secret: %s not found in %s : %s", secretId, project, e.getMessage()));
      throw e;
    }
  }
}
