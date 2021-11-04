package io.harness.secret;

import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretVersionName;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GcpSecretManager implements SecretStorage {
  private final SecretManagerServiceClient client;
  private final String project;

  public GcpSecretManager(SecretManagerServiceClient client, String project) {
    this.client = client;
    this.project = project;
  }

  @Override
  public String getSecretBy(String secretReference) throws IOException {
    try {
      SecretVersionName secretVersionName = SecretVersionName.of(project, secretReference, "latest");
      AccessSecretVersionResponse response = client.accessSecretVersion(secretVersionName);
      return response.getPayload().getData().toStringUtf8();
    } catch (Exception e) {
      log.warn(String.format("Secret: %s not found in %s : %s", secretReference, project, e.getMessage()));
      throw e;
    }
  }
}
