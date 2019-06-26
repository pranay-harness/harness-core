package io.harness.marketplace.gcp.procurement;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.util.Utils;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.cloudcommerceprocurement.v1.CloudCommercePartnerProcurementService;
import com.google.cloudcommerceprocurement.v1.CloudCommercePartnerProcurementServiceScopes;
import com.google.inject.Singleton;

import io.harness.marketplace.gcp.GcpMarketPlaceConstants;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

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
      logger.error("GCP credentials file does NOT exist. Marketplace Approval requests will fail. Path: {}", path);
      return Optional.empty();
    }

    InputStream credentialStream;
    try {
      credentialStream = Files.newInputStream(path, StandardOpenOption.READ);

    } catch (IOException e) {
      logger.error("Exception reading credentials file. Path: " + path, e);
      return Optional.empty();
    }

    GoogleCredential credentials;
    try {
      credentials = GoogleCredential.fromStream(credentialStream);
    } catch (IOException e) {
      logger.error("Exception creating google credentials from credential stream. Path: " + path, e);
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
