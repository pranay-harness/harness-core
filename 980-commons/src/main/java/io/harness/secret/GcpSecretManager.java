/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.secret;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretPayload;
import com.google.cloud.secretmanager.v1.SecretVersionName;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
public class GcpSecretManager implements SecretStorage {
  private static final String LATEST_VERSION = "latest";

  private final SecretManagerServiceClient client;
  private final String project;

  public static GcpSecretManager create(String project) throws IOException {
    return create(SecretManagerServiceClient.create(), project);
  }

  public static GcpSecretManager create(SecretManagerServiceClient client, String project) {
    return new GcpSecretManager(client, project);
  }

  protected GcpSecretManager(SecretManagerServiceClient client, String project) {
    this.client = Objects.requireNonNull(client);
    this.project = Objects.requireNonNull(project);
  }

  @Override
  public Optional<String> getSecretBy(String secretReference) throws IOException {
    try {
      log.info("Accessing secret '{}' in project '{}'...", secretReference, project);

      SecretVersionName secretVersionName = SecretVersionName.of(project, secretReference, LATEST_VERSION);

      AccessSecretVersionResponse response = client.accessSecretVersion(secretVersionName);
      return Optional.ofNullable(response.getPayload()).map(SecretPayload::getData).map(ByteString::toStringUtf8);

    } catch (Exception e) {
      log.error("Failed to access the secret. Project='{}', SecretReference='{}'", project, secretReference, e);
      return Optional.empty();
    }
  }

  @Override
  public void close() throws Exception {
    client.close();
  }
}
