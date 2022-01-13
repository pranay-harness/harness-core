/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.marketplace.gcp.procurement;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.marketplace.gcp.GcpMarketPlaceConstants;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.util.Utils;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.cloudcommerceprocurement.v1.CloudCommercePartnerProcurementService;
import com.google.cloudcommerceprocurement.v1.CloudCommercePartnerProcurementServiceScopes;
import com.google.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
@Singleton
public class ProcurementAPIClientBuilder {
  private CloudCommercePartnerProcurementService client;

  // creates procurement service client
  public Optional<CloudCommercePartnerProcurementService> getInstance() {
    if (null != this.client) {
      return Optional.of(client);
    }
    HttpTransport httpTransport = Utils.getDefaultTransport();
    JsonFactory jsonFactory = Utils.getDefaultJsonFactory();

    Path path = Paths.get(GcpMarketPlaceConstants.SERVICE_ACCOUNT_INTEGRATION_PATH);

    if (!Files.exists(path)) {
      log.error("GCP credentials file does NOT exist. Marketplace Approval requests will fail. Path: {}", path);
      return Optional.empty();
    }
    InputStream credentialStream;
    try {
      credentialStream = Files.newInputStream(path, StandardOpenOption.READ);
    } catch (IOException e) {
      log.error("Exception reading credentials file. Path: " + path, e);
      return Optional.empty();
    }
    GoogleCredential credentials;
    try {
      credentials = GoogleCredential.fromStream(credentialStream);
    } catch (IOException e) {
      log.error("Exception creating google credentials from credential stream. Path: " + path, e);
      return Optional.empty();
    }
    if (credentials.createScopedRequired()) {
      credentials = credentials.createScoped(CloudCommercePartnerProcurementServiceScopes.all());
    }
    CloudCommercePartnerProcurementService service =
        new CloudCommercePartnerProcurementService.Builder(httpTransport, jsonFactory, credentials)
            .setApplicationName("harness-gcp-marketplace")
            .build();

    this.client = service;
    return Optional.of(service);
  }
}
